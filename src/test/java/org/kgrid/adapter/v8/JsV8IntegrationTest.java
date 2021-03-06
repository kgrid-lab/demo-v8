package org.kgrid.adapter.v8;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.kgrid.adapter.api.ActivationContext;
import org.kgrid.adapter.api.Adapter;
import org.kgrid.adapter.api.AdapterException;
import org.kgrid.adapter.api.Executor;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsV8IntegrationTest {

    Adapter adapter;
    TestActivationContext activationContext;

    @BeforeEach
    public void setUp() {
        activationContext = new TestActivationContext();
        adapter = new JsV8Adapter();
        adapter.initialize(activationContext);
    }

    @Test
    public void testActivatesObjectAndGetsExecutor() throws IOException {
        JsonNode deploymentSpec = getDeploymentSpec("hello-world/deploymentSpec.yaml");
        JsonNode endpointObject = deploymentSpec.get("endpoints").get("/welcome");
        Executor executor = adapter.activate(URI.create("hello-world/"), null, endpointObject);
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("name", "Bob");
        Object helloResult = executor.execute(inputs, "application/json");
        assertEquals("Hello, Bob", helloResult);
    }

    @Test
    public void testActivatesObjectWithArrayAndGetsExecutor() throws IOException {
        JsonNode deploymentSpec = getDeploymentSpec("artifact-list-v1.0/deployment.yaml");
        JsonNode endpointObject = deploymentSpec.get("endpoints").get("/bmicalc");
        Executor executor = adapter.activate(URI.create("artifact-list-v1.0/"), null, endpointObject);
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("height", 2);
        inputs.put("weight", 80);
        Object helloResult = executor.execute(inputs, "application/json");
        assertEquals("20.0", helloResult);
    }

    @Test
    public void testActivatesObjectWithArrayWithMultipleElementsAndGetsExecutor() throws IOException {
        JsonNode deploymentSpec = getDeploymentSpec("artifact-list-v2.0/deployment.yaml");
        JsonNode endpointObject = deploymentSpec.get("endpoints").get("/bmicalc");
        Executor executor = adapter.activate(URI.create("artifact-list-v2.0/"), null, endpointObject);
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("height", 2);
        inputs.put("weight", 80);
        Object helloResult = executor.execute(inputs, "application/json");
        assertEquals("20.0", helloResult);
    }

    @Test
    public void testActivatesObjectWithArrayWithMultipleElementsNoEntryAndGetsExecutor()
            throws IOException {
        JsonNode deploymentSpec = getDeploymentSpec("artifact-list-v3.0/deployment.yaml");
        JsonNode endpointObject = deploymentSpec.get("endpoints").get("/bmicalc");
        Executor executor = adapter.activate(URI.create("artifact-list-v3.0/"), null, endpointObject);
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("height", 2);
        inputs.put("weight", 80);
        Object helloResult = executor.execute(inputs, "application/json");
        assertEquals("20.0", helloResult);
    }

    @Test
    public void testActivatesBundledJSObjectAndGetsExecutor() throws IOException {
        JsonNode deploymentSpec = getDeploymentSpec("hello-world-v1.3/deploymentSpec.yaml");
        JsonNode endpointObject = deploymentSpec.get("endpoints").get("/welcome");
        Executor executor = adapter.activate(URI.create("hello-world-v1.3/"), null, endpointObject);
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("name", "Bob");
        Object helloResult = executor.execute(inputs, "application/json");
        assertEquals("Hello, Bob", helloResult);
    }

    @Test
    public void testCantCallOtherExecutor() throws IOException {
        JsonNode deploymentSpec = getDeploymentSpec("hello-world/deploymentSpec.yaml");
        JsonNode endpointObject = deploymentSpec.get("endpoints").get("/welcome");
        Executor helloExecutor = adapter.activate(URI.create("hello-world/"), null, endpointObject);
        activationContext.addExecutor("hello-world/welcome", helloExecutor);
        deploymentSpec = getDeploymentSpec("hello-exec/deploymentSpec.yaml");
        endpointObject = deploymentSpec.get("endpoints").get("/welcome");
        Executor executor = adapter.activate(URI.create("hello-exec/"), null, endpointObject);
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("name", "Bob");
        assertThrows(
                AdapterException.class, () -> executor.execute(inputs, "application/json"));
    }

    private JsonNode getDeploymentSpec(String deploymentLocation) throws IOException {
        YAMLMapper yamlMapper = new YAMLMapper();
        ClassPathResource classPathResource = new ClassPathResource(deploymentLocation);
        return yamlMapper.readTree(classPathResource.getInputStream().readAllBytes());
    }

    @Test
    public void testActivatesTestObjectAndGetsExecutor() throws IOException {
        JsonNode deploymentSpec = getDeploymentSpec("v8-bmicalc-v1.0/deployment.yaml");
        JsonNode endpointObject = deploymentSpec.get("endpoints").get("/bmicalc");
        Executor executor = adapter.activate(URI.create("v8-bmicalc-v1.0/"), null, endpointObject);
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("height", 1.70);
        inputs.put("weight", 70);
        Object bmiResult = executor.execute(inputs, "application/json");
        assertEquals("24.2", bmiResult);
    }

    @Test
    public void testExecutiveObject() throws IOException {
        JsonNode deploymentSpec = getDeploymentSpec("hello-world/deploymentSpec.yaml");
        JsonNode endpointObject = deploymentSpec.get("endpoints").get("/welcome");
        Executor helloExecutor = adapter.activate(URI.create("hello-world/"), null, endpointObject);
        activationContext.addExecutor("hello-world/welcome", helloExecutor);

        deploymentSpec = getDeploymentSpec("v8-bmicalc-v1.0/deployment.yaml");
        endpointObject = deploymentSpec.get("endpoints").get("/bmicalc");
        helloExecutor = adapter.activate(URI.create("v8-bmicalc-v1.0/"), null, endpointObject);
        activationContext.addExecutor("v8-bmicalc-v1.0/bmicalc", helloExecutor);

        deploymentSpec = getDeploymentSpec("v8-executive-1.0/deployment.yaml");
        endpointObject = deploymentSpec.get("endpoints").get("/process");
        Executor executor = adapter.activate(URI.create("v8-executive-1.0/"), null, endpointObject);
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("name", "Bob");
        inputs.put("height", 1.70);
        inputs.put("weight", 70);
        Object helloResult =
                new ObjectMapper().writeValueAsString(executor.execute(inputs, "application/json"));
        assertEquals("{\"message\":\"Hello, Bob\",\"bmi\":\"24.2\"}", helloResult);
    }

    @Test
    public void testJsModulesObject() throws IOException {
        JsonNode deploymentSpec = getDeploymentSpec("js-modules-v1.0/deployment.yaml");
        JsonNode endpointObject = deploymentSpec.get("/welcome").get("post");
        Executor executor = adapter.activate(URI.create("src/test/resources/js-modules-v1.0/"), null, endpointObject);
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("name", "Ted");
        inputs.put("num", 2);
        Object bmiResult = executor.execute(inputs, "application/json");
        assertEquals("Welcome to the knowledge grid Ted 4", bmiResult);
    }
}

class TestActivationContext implements ActivationContext {

    Map<String, Executor> executorMap = new HashMap<>();

    @Override
    public Executor getExecutor(String executorId) {
        return executorMap.get(executorId);
    }

    @Override
    public InputStream getBinary(URI artifactUri) {
        try {
            return new ClassPathResource(artifactUri.toString()).getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getProperty(String propertyName) {
        return null;

    }

    @Override
    public void refresh(String s) {

    }

    public void addExecutor(String id, Executor executor) {
        executorMap.put(id, executor);
    }
}
