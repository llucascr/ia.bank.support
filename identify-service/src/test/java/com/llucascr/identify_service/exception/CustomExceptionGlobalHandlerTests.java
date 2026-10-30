package com.llucascr.identify_service.exception;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomExceptionGlobalHandlerTests {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new CustomExceptionGlobalHandler())
            .build();

    @ParameterizedTest
    @CsvSource({
            "/test/users/missing, 404, User not found",
            "/test/users/duplicate, 409, User already exists"
    })
    void returnsCustomErrorResponse(String path, int expectedStatus, String expectedMessage) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(expectedStatus))
                .andExpect(jsonPath("$.message").value(expectedMessage))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value(path));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/users/missing")
        public void missingUser() {
            throw new UserNotFoundException("User not found");
        }

        @GetMapping("/test/users/duplicate")
        public void duplicateUser() {
            throw new UserAlreadyExistsException("User already exists");
        }
    }
}
