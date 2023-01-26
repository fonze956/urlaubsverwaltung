package org.synyx.urlaubsverwaltung.application.export;

import com.opencsv.CSVWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.synyx.urlaubsverwaltung.application.application.Application;
import org.synyx.urlaubsverwaltung.application.application.ApplicationForLeave;
import org.synyx.urlaubsverwaltung.application.vacationtype.VacationTypeEntity;
import org.synyx.urlaubsverwaltung.period.DayLength;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.web.DateFormatAware;
import org.synyx.urlaubsverwaltung.web.FilterPeriod;
import org.synyx.urlaubsverwaltung.workingtime.WorkDaysCountService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.math.BigDecimal.TEN;
import static java.util.Locale.GERMAN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.ALLOWED;
import static org.synyx.urlaubsverwaltung.application.vacationtype.VacationCategory.HOLIDAY;

@ExtendWith(MockitoExtension.class)
class ApplicationForLeaveCsvExportServiceTest {

    private ApplicationForLeaveCsvExportService sut;

    @Mock
    private CSVWriter csvWriter;
    @Mock
    private MessageSource messageSource;
    @Mock
    private WorkDaysCountService workDaysCountService;

    @BeforeEach
    void setUp() {
        sut = new ApplicationForLeaveCsvExportService(messageSource);
    }

    @Test
    void writeApplicationForLeaveExports() {
        LocaleContextHolder.setLocale(GERMAN);

        final LocalDate startDate = LocalDate.parse("2018-01-01");
        final LocalDate endDate = LocalDate.parse("2018-12-31");
        final FilterPeriod period = new FilterPeriod(startDate, endDate);

        final Person person = new Person();
        person.setId(1);
        person.setFirstName("personOneFirstName");
        person.setLastName("personOneLastName");

        final VacationTypeEntity vacationTypeEntity = new VacationTypeEntity();
        vacationTypeEntity.setId(1);
        vacationTypeEntity.setVisibleToEveryone(true);
        vacationTypeEntity.setCategory(HOLIDAY);
        vacationTypeEntity.setMessageKey("messagekey.holiday");

        final Application application = new Application();
        application.setId(42);
        application.setPerson(person);
        application.setStartDate(startDate);
        application.setEndDate(endDate);
        application.setDayLength(DayLength.FULL);
        application.setStatus(ALLOWED);
        application.setVacationType(vacationTypeEntity);

        when(workDaysCountService.getWorkDaysCount(application.getDayLength(), application.getStartDate(), application.getEndDate(), application.getPerson())).thenReturn(TEN);

        final ApplicationForLeave applicationForLeave = new ApplicationForLeave(application, workDaysCountService);

        final List<ApplicationForLeaveExport> applicationForLeaveExports = new ArrayList<>();
        final ApplicationForLeaveExport applicationForLeaveExport = new ApplicationForLeaveExport("1", person.getFirstName(), person.getLastName(), List.of(applicationForLeave), List.of("departmentA"));
        applicationForLeaveExports.add(applicationForLeaveExport);

        addMessageSource("person.account.basedata.personnelNumber");
        addMessageSource("person.data.firstName");
        addMessageSource("person.data.lastName");
        addMessageSource("applications.export.departments");
        addMessageSource("applications.export.from");
        addMessageSource("applications.export.to");
        addMessageSource("applications.export.length");
        addMessageSource("applications.export.type");
        addMessageSource("applications.export.days");
        addMessageSource("FULL");
        addMessageSource("messagekey.holiday");

        sut.write(period, applicationForLeaveExports, csvWriter);
        verify(csvWriter).writeNext(new String[]{"{person.account.basedata.personnelNumber}", "{person.data.firstName}", "{person.data.lastName}", "{applications.export.departments}", "{applications.export.from}", "{applications.export.to}", "{applications.export.length}", "{applications.export.type}", "{applications.export.days}"});
        verify(csvWriter).writeNext(new String[]{"1", "personOneFirstName", "personOneLastName", "departmentA", "01.01.2018", "31.12.2018", "{FULL}", "{messagekey.holiday}", "10"});
    }

    private void addMessageSource(String key) {
        when(messageSource.getMessage(eq(key), any(), any())).thenReturn(String.format("{%s}", key));
    }
}
