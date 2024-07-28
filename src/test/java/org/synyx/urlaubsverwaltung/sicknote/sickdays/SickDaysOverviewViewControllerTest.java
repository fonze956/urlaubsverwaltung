package org.synyx.urlaubsverwaltung.sicknote.sickdays;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.synyx.urlaubsverwaltung.absence.DateRange;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonService;
import org.synyx.urlaubsverwaltung.search.PageableSearchQuery;
import org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNote;
import org.synyx.urlaubsverwaltung.sicknote.sicknotetype.SickNoteType;
import org.synyx.urlaubsverwaltung.web.DateFormatAware;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTimeCalendar;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTimeCalendar.WorkingDayInformation;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.math.BigDecimal.ZERO;
import static java.time.temporal.TemporalAdjusters.firstDayOfYear;
import static java.time.temporal.TemporalAdjusters.lastDayOfYear;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.synyx.urlaubsverwaltung.period.DayLength.FULL;
import static org.synyx.urlaubsverwaltung.person.Role.OFFICE;
import static org.synyx.urlaubsverwaltung.person.Role.USER;
import static org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNoteCategory.SICK_NOTE;
import static org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNoteCategory.SICK_NOTE_CHILD;
import static org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNoteStatus.ACTIVE;
import static org.synyx.urlaubsverwaltung.workingtime.WorkingTimeCalendar.WorkingDayInformation.WorkingTimeCalendarEntryType.WORKDAY;

@ExtendWith(MockitoExtension.class)
class SickDaysOverviewViewControllerTest {

    private SickDaysOverviewViewController sut;

    @Mock
    private SickDaysStatisticsService sickDaysStatisticsService;
    @Mock
    private PersonService personService;

    private static final Clock clock = Clock.systemUTC();

    @BeforeEach
    void setUp() {
        sut = new SickDaysOverviewViewController(sickDaysStatisticsService, personService, new DateFormatAware(), clock);
    }

    private static Stream<Arguments> dateInputAndIsoDateTuple() {
        final int year = Year.now(clock).getValue();
        return Stream.of(
            Arguments.of(String.format("25.03.%s", year), LocalDate.of(year, 3, 25)),
            Arguments.of(String.format("25.03.%s", year - 2000), LocalDate.of(year, 3, 25)),
            Arguments.of(String.format("25.3.%s", year), LocalDate.of(year, 3, 25)),
            Arguments.of(String.format("25.3.%s", year - 2000), LocalDate.of(year, 3, 25)),
            Arguments.of(String.format("1.4.%s", year - 2000), LocalDate.of(year, 4, 1))
        );
    }

    @ParameterizedTest
    @MethodSource("dateInputAndIsoDateTuple")
    void sickDaysRedirectsToStatisticsAfterIncorrectPeriodForStartDate(String givenDateString, LocalDate givenDate) throws Exception {

        final int year = clockYear();

        when(sickDaysStatisticsService.getAll(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        perform(get("/web/sickdays")
            .param("from", givenDateString))
            .andExpect(status().isOk())
            .andExpect(model().attribute("today", LocalDate.now(clock)))
            .andExpect(model().attribute("from", givenDate))
            .andExpect(model().attribute("to", LocalDate.of(year, 12, 31)))
            .andExpect(view().name("sicknote/sick_days"));
    }

    @ParameterizedTest
    @MethodSource("dateInputAndIsoDateTuple")
    void sickDaysRedirectsToStatisticsAfterIncorrectPeriodForEndDate(String givenDateString, LocalDate givenDate) throws Exception {

        final int year = clockYear();

        when(sickDaysStatisticsService.getAll(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        perform(get("/web/sickdays")
            .param("to", givenDateString))
            .andExpect(status().isOk())
            .andExpect(model().attribute("today", LocalDate.now(clock)))
            .andExpect(model().attribute("from", LocalDate.of(year, 1, 1)))
            .andExpect(model().attribute("to", givenDate))
            .andExpect(view().name("sicknote/sick_days"));
    }

    @Test
    void filterSickNotesWithNullDates() throws Exception {

        final int year = Year.now(clock).getValue();

        when(sickDaysStatisticsService.getAll(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        perform(get("/web/sickdays"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("today", LocalDate.now(clock)))
            .andExpect(model().attribute("from", LocalDate.of(year, 1, 1)))
            .andExpect(model().attribute("to", LocalDate.of(year, 12, 31)))
            .andExpect(view().name("sicknote/sick_days"));
    }

    @Test
    void periodsSickNotesWithDateRangeWithRole() throws Exception {

        final Person office = new Person();
        office.setId(1L);
        office.setPermissions(List.of(USER, OFFICE));
        when(personService.getSignedInUser()).thenReturn(office);

        final Person person = new Person();
        person.setId(1L);
        person.setFirstName("FirstName one");
        person.setLastName("LastName one");
        person.setPermissions(List.of(USER));

        final Person person2 = new Person();
        person2.setId(2L);
        person2.setFirstName("FirstName two");
        person2.setLastName("LastName two");
        person2.setPermissions(List.of(USER));

        final Person person3 = new Person();
        person3.setId(3L);
        person3.setFirstName("FirstName three");
        person3.setLastName("LastName three");
        person3.setPermissions(List.of(USER));

        final Map<LocalDate, WorkingDayInformation> workingTimes = buildWorkingTimeByDate(
            LocalDate.of(2019, 1, 1),
            LocalDate.of(2019, 12, 31),
            (date) -> new WorkingDayInformation(FULL, WORKDAY, WORKDAY)
        );
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(workingTimes);

        final SickNoteType childSickType = new SickNoteType();
        childSickType.setCategory(SICK_NOTE_CHILD);

        final SickNote childSickNote = SickNote.builder()
            .startDate(LocalDate.of(2019, 2, 1))
            .endDate(LocalDate.of(2019, 3, 1))
            .dayLength(FULL)
            .status(ACTIVE)
            .sickNoteType(childSickType)
            .person(person)
            .aubStartDate(LocalDate.of(2019, 2, 10))
            .aubEndDate(LocalDate.of(2019, 2, 15))
            .workingTimeCalendar(workingTimeCalendar)
            .build();

        final SickNoteType sickType = new SickNoteType();
        sickType.setCategory(SICK_NOTE);

        final SickNote sickNote = SickNote.builder()
            .startDate(LocalDate.of(2019, 4, 1))
            .endDate(LocalDate.of(2019, 5, 1))
            .dayLength(FULL)
            .status(ACTIVE)
            .sickNoteType(sickType)
            .person(person2)
            .aubStartDate(LocalDate.of(2019, 4, 10))
            .aubEndDate(LocalDate.of(2019, 4, 20))
            .workingTimeCalendar(workingTimeCalendar)
            .build();

        final LocalDate requestStartDate = LocalDate.of(2019, 2, 11);
        final LocalDate requestEndDate = LocalDate.of(2019, 4, 15);

        final PageableSearchQuery pageableSearchQuery =
            new PageableSearchQuery(PageRequest.of(2, 50, Sort.by(Sort.Direction.ASC, "person.firstName")), "");

        final SickDaysDetailedStatistics statisticsPersonOne = new SickDaysDetailedStatistics("0000001337", person, List.of(sickNote), List.of());
        final SickDaysDetailedStatistics statisticsPersonTwo = new SickDaysDetailedStatistics("0000000042", person2, List.of(childSickNote), List.of());
        final SickDaysDetailedStatistics statisticsPersonThree = new SickDaysDetailedStatistics("0000000021", person3, List.of(), List.of());

        when(sickDaysStatisticsService.getAll(office, requestStartDate, requestEndDate, pageableSearchQuery))
            .thenReturn(new PageImpl<>(List.of(statisticsPersonOne, statisticsPersonTwo, statisticsPersonThree)));

        perform(get("/web/sickdays")
            .param("from", requestStartDate.toString())
            .param("to", requestEndDate.toString())
            .param("page", "2")
            .param("size", "50")
        )
            .andExpect(status().isOk())
            .andExpect(model().attribute("sickDaysStatistics", contains(
                allOf(
                    hasProperty("personId", is(1L)),
                    hasProperty("personAvatarUrl", is("")),
                    hasProperty("personnelNumber", is("0000001337")),
                    hasProperty("personFirstName", is("FirstName one")),
                    hasProperty("personLastName", is("LastName one")),
                    hasProperty("personNiceName", is("FirstName one LastName one")),
                    hasProperty("amountSickDays", is(BigDecimal.valueOf(15))),
                    hasProperty("amountSickDaysWithAUB", is(BigDecimal.valueOf(6))),
                    hasProperty("amountChildSickDays", is(ZERO)),
                    hasProperty("amountChildSickDaysWithAUB", is(ZERO))
                ),
                allOf(
                    hasProperty("personId", is(2L)),
                    hasProperty("personAvatarUrl", is("")),
                    hasProperty("personnelNumber", is("0000000042")),
                    hasProperty("personFirstName", is("FirstName two")),
                    hasProperty("personLastName", is("LastName two")),
                    hasProperty("personNiceName", is("FirstName two LastName two")),
                    hasProperty("amountSickDays", is(ZERO)),
                    hasProperty("amountSickDaysWithAUB", is(ZERO)),
                    hasProperty("amountChildSickDays", is(BigDecimal.valueOf(19))),
                    hasProperty("amountChildSickDaysWithAUB", is(BigDecimal.valueOf(5)))
                ),
                allOf(
                    hasProperty("personId", is(3L)),
                    hasProperty("personAvatarUrl", is("")),
                    hasProperty("personnelNumber", is("0000000021")),
                    hasProperty("personFirstName", is("FirstName three")),
                    hasProperty("personLastName", is("LastName three")),
                    hasProperty("personNiceName", is("FirstName three LastName three")),
                    hasProperty("amountSickDays", is(ZERO)),
                    hasProperty("amountSickDaysWithAUB", is(ZERO)),
                    hasProperty("amountChildSickDays", is(ZERO)),
                    hasProperty("amountChildSickDaysWithAUB", is(ZERO))
                )
            )))
            .andExpect(model().attribute("showPersonnelNumberColumn", true))
            .andExpect(model().attribute("from", requestStartDate))
            .andExpect(model().attribute("to", requestEndDate))
            .andExpect(model().attribute("period", hasProperty("startDate", is(requestStartDate))))
            .andExpect(model().attribute("period", hasProperty("endDate", is(requestEndDate))))
            .andExpect(view().name("sicknote/sick_days"));
    }

    @Test
    void periodsSickNotesWithDateWithoutRange() throws Exception {

        final Person office = new Person();
        office.setId(1L);
        office.setPermissions(List.of(USER, OFFICE));
        when(personService.getSignedInUser()).thenReturn(office);

        final int year = Year.now(clock).getValue();
        final LocalDate startDate = ZonedDateTime.now(clock).withYear(year).with(firstDayOfYear()).toLocalDate();
        final LocalDate endDate = ZonedDateTime.now(clock).withYear(year).with(lastDayOfYear()).toLocalDate();

        final PageableSearchQuery pageableSearchQuery =
            new PageableSearchQuery(PageRequest.of(1, 50, Sort.by(Sort.Direction.ASC, "person.firstName")), "");

        when(sickDaysStatisticsService.getAll(office, startDate, endDate, pageableSearchQuery))
            .thenReturn(new PageImpl<>(List.of()));

        final ResultActions resultActions = perform(get("/web/sickdays")
            .param("from", "01.01." + year)
            .param("to", "31.12." + year)
            .param("page", "1")
            .param("size", "50")
        );

        resultActions
            .andExpect(status().isOk())
            .andExpect(model().attribute("from", startDate))
            .andExpect(model().attribute("to", endDate))
            .andExpect(model().attribute("period", hasProperty("startDate", is(startDate))))
            .andExpect(model().attribute("period", hasProperty("endDate", is(endDate))))
            .andExpect(view().name("sicknote/sick_days"));
    }

    @Test
    void sickNotesWithoutPersonnelNumberColumn() throws Exception {

        final Person signedInUser = new Person();
        signedInUser.setId(1L);
        signedInUser.setPermissions(List.of(USER));
        when(personService.getSignedInUser()).thenReturn(signedInUser);

        final LocalDate requestStartDate = LocalDate.of(2019, 2, 11);
        final LocalDate requestEndDate = LocalDate.of(2019, 4, 15);

        final PageableSearchQuery pageableSearchQuery =
            new PageableSearchQuery(PageRequest.of(2, 50, Sort.by(Sort.Direction.ASC, "person.firstName")), "");

        final SickDaysDetailedStatistics statistics = new SickDaysDetailedStatistics("", signedInUser, List.of(), List.of());
        when(sickDaysStatisticsService.getAll(signedInUser, requestStartDate, requestEndDate, pageableSearchQuery))
            .thenReturn(new PageImpl<>(List.of(statistics)));

        perform(get("/web/sickdays")
            .param("from", requestStartDate.toString())
            .param("to", requestEndDate.toString())
            .param("page", "2")
            .param("size", "50")
        )
            .andExpect(model().attribute("showPersonnelNumberColumn", false))
            .andExpect(view().name("sicknote/sick_days"));
    }

    private Map<LocalDate, WorkingDayInformation> buildWorkingTimeByDate(LocalDate from, LocalDate to, Function<LocalDate, WorkingDayInformation> dayLengthProvider) {
        final Map<LocalDate, WorkingDayInformation> map = new HashMap<>();
        for (LocalDate date : new DateRange(from, to)) {
            map.put(date, dayLengthProvider.apply(date));
        }
        return map;
    }

    private static int clockYear() {
        return Year.now(clock).getValue();
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build()
            .perform(builder);
    }
}
