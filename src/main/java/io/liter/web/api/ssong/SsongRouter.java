package io.liter.web.api.ssong;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class SsongRouter {


    @Bean
    public RouterFunction<ServerResponse> sampleRouterFunction(SsongHandler handler) {

        return RouterFunctions
                .nest(path("/ssong"),
                        route(GET(""), handler::get)
                                .andRoute(POST("/tag"), handler::postTag)
                                .andRoute(POST("/review"), handler::postReview)
                );
    }
}
