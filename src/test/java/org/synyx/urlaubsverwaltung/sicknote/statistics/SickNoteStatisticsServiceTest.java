package org.synyx.urlaubsverwaltung.sicknote.statistics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.synyx.urlaubsverwaltung.department.DepartmentService;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonId;
import org.synyx.urlaubsverwaltung.person.Role;
import org.synyx.urlaubsverwaltung.person.basedata.PersonBasedata;
import org.synyx.urlaubsverwaltung.person.basedata.PersonBasedataService;
import org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNote;
import org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNoteService;
import org.synyx.urlaubsverwaltung.workingtime.WorkDaysCountService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static java.math.BigDecimal.ONE;
import static java.time.temporal.TemporalAdjusters.lastDayOfYear;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.synyx.urlaubsverwaltung.person.Role.DEPARTMENT_HEAD;
import static org.synyx.urlaubsverwaltung.person.Role.SECOND_STAGE_AUTHORITY;
import static org.synyx.urlaubsverwaltung.person.Role.USER;
import static org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNoteStatus.ACTIVE;

@ExtendWith(MockitoExtension.class)
class SickNoteStatisticsServiceTest {

    private SickNoteStatisticsService sut;

    @Mock
    private SickNoteService sickNoteService;
    @Mock
    private WorkDaysCountService workDaysCountService;
    @Mock
    private DepartmentService departmentService;
    @Mock
    private PersonBasedataService personBasedataService;

    @BeforeEach
    void setUp() {
        sut = new SickNoteStatisticsService(sickNoteService, workDaysCountService, departmentService, personBasedataService);
    }

    @Test
    void ensureCreateStatisticsForPersonWithRoleDepartmentHeadOnlyForMembers() {

        final Clock fixedClock = Clock.fixed(Instant.parse("2022-10-17T00:00:00.00Z"), ZoneId.systemDefault());

        final Person departmentHead = new Person();
        departmentHead.setPermissions(List.of(USER, DEPARTMENT_HEAD));

        final Person member1 = new Person();
        final Person member2 = new Person();
        final List<Person> members = List.of(member1, member2);
        when(departmentService.getMembersForDepartmentHead(departmentHead)).thenReturn(members);

        final LocalDate firstDayOfYear = Year.now(fixedClock).atDay(1);
        final LocalDate lastDayOfYear = firstDayOfYear.with(lastDayOfYear());
        final SickNote sickNote = new SickNote();
        sickNote.setPerson(member1);
        sickNote.setStartDate(LocalDate.of(2022, 10, 10));
        sickNote.setEndDate(LocalDate.of(2022, 10, 10));
        final List<SickNote> sickNotes = List.of(sickNote);
        when(sickNoteService.getForStatesAndPerson(List.of(ACTIVE), members, firstDayOfYear, lastDayOfYear)).thenReturn(sickNotes);
        when(workDaysCountService.getWorkDaysCount(any(), any(), any(), any())).thenReturn(ONE);

        final SickNoteStatistics sickNoteStatistics = sut.createStatisticsForPerson(departmentHead, fixedClock);
        assertThat(sickNoteStatistics.getTotalNumberOfSickNotes()).isOne();
        assertThat(sickNoteStatistics.getNumberOfPersonsWithMinimumOneSickNote()).isOne();
    }

    @Test
    void ensureCreateStatisticsForPersonWithRoleSecondStageAuthorityOnlyForMembers() {

        final Clock fixedClock = Clock.fixed(Instant.parse("2022-10-17T00:00:00.00Z"), ZoneId.systemDefault());

        final Person ssa = new Person();
        ssa.setPermissions(List.of(USER, SECOND_STAGE_AUTHORITY));

        final Person member1 = new Person();
        final Person member2 = new Person();
        final List<Person> members = List.of(member1, member2);
        when(departmentService.getMembersForSecondStageAuthority(ssa)).thenReturn(members);

        final LocalDate firstDayOfYear = Year.now(fixedClock).atDay(1);
        final LocalDate lastDayOfYear = firstDayOfYear.with(lastDayOfYear());
        final SickNote sickNote = new SickNote();
        sickNote.setPerson(member1);
        sickNote.setStartDate(LocalDate.of(2022, 10, 10));
        sickNote.setEndDate(LocalDate.of(2022, 10, 10));
        final List<SickNote> sickNotes = List.of(sickNote);
        when(sickNoteService.getForStatesAndPerson(List.of(ACTIVE), members, firstDayOfYear, lastDayOfYear)).thenReturn(sickNotes);
        when(workDaysCountService.getWorkDaysCount(any(), any(), any(), any())).thenReturn(ONE);

        final SickNoteStatistics sickNoteStatistics = sut.createStatisticsForPerson(ssa, fixedClock);
        assertThat(sickNoteStatistics.getTotalNumberOfSickNotes()).isOne();
        assertThat(sickNoteStatistics.getNumberOfPersonsWithMinimumOneSickNote()).isOne();
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"OFFICE", "BOSS"})
    void ensureCreateStatisticsForPersonWithRole(Role role) {

        final Clock fixedClock = Clock.fixed(Instant.parse("2022-10-17T00:00:00.00Z"), ZoneId.systemDefault());

        final Person personWithRole = new Person();
        personWithRole.setPermissions(List.of(USER, role));

        final Person person = new Person();
        final LocalDate from = LocalDate.of(2022, 1, 1);
        final LocalDate to = LocalDate.of(2022, 12, 31);

        final SickNote sickNote = new SickNote();
        sickNote.setPerson(person);
        sickNote.setStartDate(LocalDate.of(2022, 10, 10));
        sickNote.setEndDate(LocalDate.of(2022, 10, 10));
        final List<SickNote> sickNotes = List.of(sickNote);
        when(sickNoteService.getAllActiveByPeriod(from, to)).thenReturn(sickNotes);
        when(workDaysCountService.getWorkDaysCount(any(), any(), any(), any())).thenReturn(ONE);

        final SickNoteStatistics sickNoteStatistics = sut.createStatisticsForPerson(personWithRole, fixedClock);
        assertThat(sickNoteStatistics.getTotalNumberOfSickNotes()).isOne();
        assertThat(sickNoteStatistics.getNumberOfPersonsWithMinimumOneSickNote()).isOne();
    }

    @Test
    void ensureCreateStatisticsForPersonWithoutPrivilegedRole() {

        final Clock fixedClock = Clock.fixed(Instant.parse("2022-10-17T00:00:00.00Z"), ZoneId.systemDefault());

        final Person person = new Person();
        person.setPermissions(List.of(USER));

        final SickNoteStatistics sickNoteStatistics = sut.createStatisticsForPerson(person, fixedClock);
        assertThat(sickNoteStatistics.getTotalNumberOfSickNotes()).isZero();
        assertThat(sickNoteStatistics.getNumberOfPersonsWithMinimumOneSickNote()).isZero();
    }

    @Test
    void getAllSickNotesAsDepartmentHead() {

        final LocalDate startDate = LocalDate.parse("2022-01-01");
        final LocalDate endDate = LocalDate.parse("2022-12-31");

        final Person departmentHead = new Person();
        departmentHead.setPermissions(List.of(USER, DEPARTMENT_HEAD));
        departmentHead.setFirstName("Department");
        departmentHead.setLastName("Head");
        departmentHead.setId(42);

        final String personnnelNumber = "Passagier1337";
        final PersonBasedata personBasedata = new PersonBasedata(new PersonId(departmentHead.getId()), personnnelNumber, "additionalInfo");

        final SickNote sickNote = new SickNote();
        sickNote.setPerson(departmentHead);
        sickNote.setStartDate(startDate.plusDays(5));
        sickNote.setEndDate(startDate.plusDays(6));

        final Person member = new Person();
        when(departmentService.getMembersForDepartmentHead(departmentHead)).thenReturn(List.of(member));
        when(sickNoteService.getForStatesAndPerson(List.of(ACTIVE), List.of(member), startDate, endDate)).thenReturn(List.of(sickNote));

        final Map<PersonId, PersonBasedata> personIdBasedatamap = Map.of(new PersonId(departmentHead.getId()), personBasedata);
        when(personBasedataService.getBasedataByPersonId(List.of(departmentHead.getId()))).thenReturn(personIdBasedatamap);

        when(departmentService.getDepartmentNamesByMembers(List.of(departmentHead))).thenReturn(Map.of(new PersonId(departmentHead.getId()), List.of("Kitchen", "Service")));

        final List<SickNoteDetailedStatistics> allSicknotes = sut.getAllSickNotes(departmentHead, startDate, endDate);
        assertThat(allSicknotes)
            .extracting(SickNoteDetailedStatistics::getPersonalNumber,
                SickNoteDetailedStatistics::getFirstName,
                SickNoteDetailedStatistics::getLastName,
                SickNoteDetailedStatistics::getDepartments,
                SickNoteDetailedStatistics::getSickNotes
            )
            .contains(tuple(personnnelNumber, "Department", "Head", List.of("Kitchen", "Service"), List.of(sickNote)));
    }
}
