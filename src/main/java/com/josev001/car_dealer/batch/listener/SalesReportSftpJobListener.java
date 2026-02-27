package com.josev001.car_dealer.batch.listener;

import com.josev001.car_dealer.batch.decider.InputFilesDecider;
import com.josev001.car_dealer.integration.SftpDownloadService;
import com.josev001.car_dealer.integration.SftpUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class SalesReportSftpJobListener implements JobExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SalesReportSftpJobListener.class);

    private final SftpDownloadService sftpDownloadService;
    private final SftpUploadService sftpUploadService;

    // Injeta os servicos responsaveis pelo fluxo SFTP do job.
    public SalesReportSftpJobListener(SftpDownloadService sftpDownloadService,
                                      SftpUploadService sftpUploadService) {
        this.sftpDownloadService = sftpDownloadService;
        this.sftpUploadService = sftpUploadService;
    }

    // Baixa os arquivos no inicio do job e registra a quantidade baixada.
    @Override
    public void beforeJob(JobExecution jobExecution) {
        LOGGER.info("Iniciando download de arquivos no SFTP antes do job");
        int downloadedFilesCount = sftpDownloadService.downloadNewFiles();
        jobExecution.getExecutionContext()
                .putInt(InputFilesDecider.DOWNLOADED_FILES_COUNT_CONTEXT_KEY, downloadedFilesCount);
        LOGGER.info("Download SFTP finalizado com {} arquivo(s) novo(s).", downloadedFilesCount);
    }

    // Executa upload e movimentacao SFTP somente quando o job conclui com sucesso.
    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            LOGGER.warn("Job finalizado com status {}. Upload/movimentacao SFTP nao sera executado.",
                    jobExecution.getStatus());
            return;
        }

        if (InputFilesDecider.NO_INPUT.equals(jobExecution.getExitStatus().getExitCode())) {
            LOGGER.info("Job finalizado com status {}. Upload/movimentacao SFTP nao sera executado.",
                    InputFilesDecider.NO_INPUT);
            return;
        }

        LOGGER.info("Job concluido com sucesso. Iniciando upload e movimentacao no SFTP");
        sftpUploadService.uploadAndMoveProcessedFiles();
    }
}

