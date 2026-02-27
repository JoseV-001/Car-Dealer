package com.josev001.car_dealer.batch.decider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class InputFilesDecider implements JobExecutionDecider {

    public static final String NO_INPUT = "NO_INPUT";
    public static final String DOWNLOADED_FILES_COUNT_CONTEXT_KEY = "downloadedFilesCount";

    private static final Logger LOGGER = LoggerFactory.getLogger(InputFilesDecider.class);

    private final ResourcePatternResolver resourcePatternResolver;
    private final String filialReportPattern;

    // Recebe o resolver e o pattern usado para localizar arquivos de entrada.
    public InputFilesDecider(ResourcePatternResolver resourcePatternResolver,
                             @Value("${app.filial-report-pattern}") String filialReportPattern) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.filialReportPattern = filialReportPattern;
    }

    // Decide se o job deve seguir com processamento ou finalizar sem entrada.
    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        ExecutionContext executionContext = jobExecution.getExecutionContext();
        if (executionContext.containsKey(DOWNLOADED_FILES_COUNT_CONTEXT_KEY)) {
            int downloadedFilesCount = executionContext.getInt(DOWNLOADED_FILES_COUNT_CONTEXT_KEY, 0);
            if (downloadedFilesCount == 0) {
                LOGGER.info("Nenhum arquivo novo foi baixado do SFTP. Finalizando job com status {}.", NO_INPUT);
                return new FlowExecutionStatus(NO_INPUT);
            }

            LOGGER.info("Foram baixados {} arquivo(s) novo(s) do SFTP. Job seguira para processamento.",
                    downloadedFilesCount);
            return FlowExecutionStatus.COMPLETED;
        }

        try {
            Resource[] resources = resourcePatternResolver.getResources(filialReportPattern);
            int filesCount = 0;
            for (Resource resource : resources) {
                if (resource.exists()) {
                    filesCount++;
                }
            }

            if (filesCount == 0) {
                LOGGER.info("Nenhum arquivo encontrado em '{}'. Finalizando job com status {}.",
                        filialReportPattern, NO_INPUT);
                return new FlowExecutionStatus(NO_INPUT);
            }

            LOGGER.info("Foram encontrados {} arquivo(s) para processamento em '{}'.", filesCount, filialReportPattern);
            return FlowExecutionStatus.COMPLETED;
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao verificar arquivos de entrada com pattern: " + filialReportPattern, e);
        }
    }
}
