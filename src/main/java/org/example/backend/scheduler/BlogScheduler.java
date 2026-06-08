package org.example.backend.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.service.IBlogService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlogScheduler {

    private final IBlogService blogService;

    // Run every minute
    @Scheduled(cron = "0 * * * * *")
    public void publishScheduledBlogs() {
        log.debug("Running scheduled task to publish blogs...");
        try {
            blogService.publishScheduledBlogs();
        } catch (Exception e) {
            log.error("Error publishing scheduled blogs", e);
        }
    }
}
