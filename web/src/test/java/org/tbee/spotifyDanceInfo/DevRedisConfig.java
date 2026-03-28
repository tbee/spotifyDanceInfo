package org.tbee.spotifyDanceInfo;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import com.redis.testcontainers.RedisContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CompletableFuture;

@Configuration(proxyBeanMethods = false)
public class DevRedisConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(DevRedisConfig.class);

    @Bean
    @ServiceConnection("redis")
//    @Profile("dev")
    GenericContainer<?> redisContainer() {
        // https://maciejwalkowiak.com/blog/testcontainers-spring-boot-container-logs/
        // container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("postgres")));
        // redisContainer.withExposedPorts(6379);

        RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:6.2.6"));
        redisContainer.start();
        redisContainer.followOutput(new Slf4jLogConsumer(LOGGER)
                .withSeparateOutputStreams()
                .withPrefix("REDIS"));

        // This should log Set and Get.
        // Unlike a thread, a future can be cancelled.
        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LOGGER)
                .withSeparateOutputStreams()
                .withPrefix("REDIS-MONITOR");
        redisContainer.followOutput(logConsumer);
        CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("Starting Redis monitor");

                DockerClient dockerClient = redisContainer.getDockerClient();
                String containerId = redisContainer.getContainerId();

                // 1. Create the exec instance
                ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                        .withCmd("redis-cli", "monitor")
                        .withAttachStdout(true)
                        .withAttachStderr(true)
                        .exec();

                // 2. Start it and stream output line-by-line
                dockerClient.execStartCmd(execCreateCmdResponse.getId())
                        .exec(new ResultCallback.Adapter<Frame>() {
                            @Override
                            public void onNext(Frame frame) {
                                String line = new String(frame.getPayload()).stripTrailing();
                                LOGGER.info("[REDIS-MONITOR] {}", line);
                            }
                        })
                        .awaitCompletion(); // blocks until monitor exits (i.e. container stops)

                LOGGER.info("Stopped Redis monitor");
            }
            catch (Exception e) {
                if (!Thread.currentThread().isInterrupted()) {
                    LOGGER.error("Redis monitor failed", e);
                }
            }
        });

//        // Give the monitor a moment to connect before issuing commands
//        try {
//            Thread.sleep(500);
//        }
//        catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//
//        // Connect to Redis
//        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisContainer.getHost(), redisContainer.getMappedPort(6379));
//        factory.afterPropertiesSet();   // important for initialization
//        StringRedisTemplate template = new StringRedisTemplate(factory);
//        template.afterPropertiesSet();
//        template.opsForValue().set("test:key", "Hello from Spring Boot + Testcontainers");
//        String value = template.opsForValue().get("test:key");
//        LOGGER.info("Retrieved value: {}", value);

        return redisContainer;
    }
}