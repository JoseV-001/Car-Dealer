package com.josev001.car_dealer.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class SalesReportScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SalesReportScheduler.class);

    private final JobOperator jobOperator;
    private final Job salesReportJob;

    // Injeta o operador e o job usados para disparar o processamento.
    public SalesReportScheduler(JobOperator jobOperator, Job salesReportJob) {
        this.jobOperator = jobOperator;
        this.salesReportJob = salesReportJob;
    }

    // Dispara uma nova execucao do job com parametro de data/hora atual.
    @Scheduled(fixedRate = 60_000)
    public void run() throws JobInstanceAlreadyCompleteException, InvalidJobParametersException, JobExecutionAlreadyRunningException, JobRestartException {
        LOGGER.info("Iniciando execução diária");

        jobOperator.start(
                salesReportJob,
                new JobParametersBuilder()
                        .addLocalDateTime("startDate", LocalDateTime.now(ZoneId.systemDefault()))
                        .toJobParameters()
        );

    }

}
