package cd.go.contrib.elasticagents.openstack.executors;

import cd.go.contrib.elasticagents.openstack.*;
import cd.go.contrib.elasticagents.openstack.model.JobIdentifier;
import cd.go.contrib.elasticagents.openstack.requests.CreateAgentRequest;
import cd.go.contrib.elasticagents.openstack.utils.ImageNotFoundException;
import cd.go.contrib.elasticagents.openstack.utils.OpenstackClientWrapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class CreateAgentRequestExecutorTest {
    private static final String IMAGE_ID = "7637f039-027d-471f-8d6c-4177635f84f8";
    private static final String FLAVOR_ID = "5";
    private CreateAgentRequest createAgentRequest;
    private AgentInstances<OpenStackInstance> agentInstances;
    private PluginRequest pluginRequest;
    private Agents agents;
    private PluginSettings pluginSettings;
    private OpenstackClientWrapper openstackClientWrapper;
    private PendingAgentsService pendingAgents;
    private OpenStackInstance osInstance;
    private JobIdentifier job1;
    private JobIdentifier job2;

    @Before
    public void SetUp() throws ImageNotFoundException {
        createAgentRequest = mock(CreateAgentRequest.class);
        agentInstances = mock(AgentInstances.class);
        pluginRequest = mock(PluginRequest.class);
        pendingAgents = mock(PendingAgentsService.class);
        when(pendingAgents.getAgents()).thenReturn(new PendingAgent[0]);
        agents = new Agents();
        openstackClientWrapper = mock(OpenstackClientWrapper.class);
        pluginSettings = new PluginSettings();
        pluginSettings.setOpenstackEndpoint("http://some/url");
        pluginSettings.setOpenstackFlavor("default-flavor");
        pluginSettings.setOpenstackImage(IMAGE_ID);
        pluginSettings.setOpenstackNetwork("780f2cfc-389b-4cc5-9b85-ed03a73975ee");
        pluginSettings.setOpenstackPassword("secret");
        pluginSettings.setOpenstackUser("user");
        pluginSettings.setOpenstackTenant("tenant");
        pluginSettings.setOpenstackVmPrefix("prefix-");
        osInstance = mock(OpenStackInstance.class);
        when(osInstance.getImageIdOrName()).thenReturn(IMAGE_ID);
        when(osInstance.getFlavorIdOrName()).thenReturn(FLAVOR_ID);
        when(openstackClientWrapper.getImageId(anyString(), anyString())).thenReturn(IMAGE_ID);
        when(openstackClientWrapper.getFlavorId(anyString(), anyString())).thenReturn(FLAVOR_ID);
        job1 = mock(JobIdentifier.class);
        populateJobFields(job1);
        job2 = mock(JobIdentifier.class);
        populateJobFields(job2);
        when(job2.getJobId()).thenReturn(101L);
    }

    private void populateJobFields(JobIdentifier job) {
        when(job.getJobId()).thenReturn(100L);
        when(job.getPipelineName()).thenReturn("build");
        when(job.getPipelineLabel()).thenReturn("build");
        when(job.getPipelineCounter()).thenReturn(1L);
        when(job.getStageName()).thenReturn("test-stage");
        when(job.getStageCounter()).thenReturn("1");
        when(job.getJobName()).thenReturn("test-job1");
    }

    @Test
    public void executeShouldCreateAgentWhenNoAgentsExist() throws Exception {
        // Arrange
        when(pendingAgents.getAgents()).thenReturn(new PendingAgent[0]);
        when(pluginRequest.listAgents()).thenReturn(agents);
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), anyString(), any(PluginSettings.class),
                any(OpenstackClientWrapper.class), anyString(), anyBoolean())).thenReturn(true);
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper, pendingAgents);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, times(1)).create(any(CreateAgentRequest.class), any(PluginSettings.class), anyString());
    }

    @Test
    public void executeShouldNotCreateAgentWhenPendingAgentsExistsForSameJob() throws Exception {
        // Arrange
        PendingAgent[] pending = new PendingAgent[1];
        Map<String, String> props = new HashMap<>();
        CreateAgentRequest originalRequest = new CreateAgentRequest("123", props, job1, null);
        pending[0] = new PendingAgent(osInstance, originalRequest);
        when(pendingAgents.getAgents()).thenReturn(pending);
        when(pluginRequest.listAgents()).thenReturn(agents);
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), anyString(), any(PluginSettings.class),
                any(OpenstackClientWrapper.class), anyString(), anyBoolean())).thenReturn(false);
        when(createAgentRequest.job()).thenReturn(job1);
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper, pendingAgents);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, never()).create(any(CreateAgentRequest.class), any(PluginSettings.class), anyString());
    }

    @Test
    public void executeShouldCreateAgentWhenPendingAgentsExistsForAnotherJob() throws Exception {
        // Arrange
        PendingAgent[] pending = new PendingAgent[1];
        Map<String, String> props = new HashMap<>();
        CreateAgentRequest originalRequest = new CreateAgentRequest("123", props, job1, null);
        pending[0] = new PendingAgent(osInstance, originalRequest);
        when(pendingAgents.getAgents()).thenReturn(pending);
        when(pluginRequest.listAgents()).thenReturn(agents);
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), anyString(), any(PluginSettings.class),
                any(OpenstackClientWrapper.class), anyString(), anyBoolean())).thenReturn(false);
        when(createAgentRequest.job()).thenReturn(job2);
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper, pendingAgents);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, times(1)).create(any(CreateAgentRequest.class), any(PluginSettings.class), anyString());
    }

    @Test
    public void executeShouldCreateAgentWhenOnlyBuildingAgentsExist() throws Exception {
        // Arrange
        agents.add(new Agent("id1", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        when(pluginRequest.listAgents()).thenReturn(agents);
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), anyString(), any(PluginSettings.class),
                any(OpenstackClientWrapper.class), anyString(), anyBoolean())).thenReturn(true);
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper, pendingAgents);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, times(1)).create(any(CreateAgentRequest.class), any(PluginSettings.class), anyString());
    }

    @Test
    public void executeShouldCreateAgentWhenOnlyAgentsWithoutEnvironmentExist() throws Exception {
        // Arrange
        agents.add(new Agent("id1", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        when(pluginRequest.listAgents()).thenReturn(agents);
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), anyString(), any(PluginSettings.class),
                any(OpenstackClientWrapper.class), anyString(), anyBoolean())).thenReturn(true);
        Map<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_MAX_INSTANCE_LIMIT, "3");
        createAgentRequest = new CreateAgentRequest("abc-key", properties, job1, "testing");
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper, pendingAgents);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, times(1)).create(any(CreateAgentRequest.class), any(PluginSettings.class), anyString());
    }

    @Test
    public void executeShouldCreateAgentWhenOnlyIdleAgentExist() throws Exception {
        // Arrange
        agents.add(new Agent("id1", Agent.AgentState.Idle, Agent.BuildState.Idle, Agent.ConfigState.Enabled));
        agents.add(new Agent("id2", Agent.AgentState.Idle, Agent.BuildState.Idle, Agent.ConfigState.Enabled));
        when(pluginRequest.listAgents()).thenReturn(agents);
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), anyString(), any(PluginSettings.class),
                any(OpenstackClientWrapper.class), anyString(), anyBoolean())).thenReturn(true);
        Map<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_MIN_INSTANCE_LIMIT, "3");
        properties.put(Constants.OPENSTACK_MAX_INSTANCE_LIMIT, "");
        createAgentRequest = new CreateAgentRequest("abc-key", properties, job1, "");
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper, pendingAgents);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, atLeastOnce()).create(any(CreateAgentRequest.class), any(PluginSettings.class), anyString());
    }

    @Test
    public void executeShouldNotCreateAgentWhenThreeIdleAgentExist() throws Exception {
        // Arrange
        agents.add(new Agent("id1", Agent.AgentState.Idle, Agent.BuildState.Idle, Agent.ConfigState.Enabled));
        agents.add(new Agent("id2", Agent.AgentState.Idle, Agent.BuildState.Idle, Agent.ConfigState.Enabled));
        agents.add(new Agent("id3", Agent.AgentState.Idle, Agent.BuildState.Idle, Agent.ConfigState.Enabled));
        when(pluginRequest.listAgents()).thenReturn(agents);
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), anyString(), any(PluginSettings.class),
                any(OpenstackClientWrapper.class), anyString(), anyBoolean())).thenReturn(true);
        Map<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_MAX_INSTANCE_LIMIT, "");
        createAgentRequest = new CreateAgentRequest("abc-key", properties, job1, "");
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper, pendingAgents);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, never()).create(any(CreateAgentRequest.class), any(PluginSettings.class), anyString());
    }

    @Test
    public void executeShouldNotCreateAgentWhenIdleAndBuildingAgentExist() throws Exception {
        // Arrange
        agents.add(new Agent("id1", Agent.AgentState.Idle, Agent.BuildState.Idle, Agent.ConfigState.Enabled));
        agents.add(new Agent("id2", Agent.AgentState.Idle, Agent.BuildState.Idle, Agent.ConfigState.Enabled));
        agents.add(new Agent("id3", Agent.AgentState.Idle, Agent.BuildState.Idle, Agent.ConfigState.Enabled));
        agents.add(new Agent("id4", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        when(pluginRequest.listAgents()).thenReturn(agents);
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        Map<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_MIN_INSTANCE_LIMIT, "3");
        createAgentRequest = new CreateAgentRequest("abc-key", properties, job1, "");
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), anyString(), any(PluginSettings.class),
                any(OpenstackClientWrapper.class), anyString(), anyBoolean())).thenReturn(true);
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper, pendingAgents);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, never()).create(any(CreateAgentRequest.class), any(PluginSettings.class), anyString());
    }

    @Test
    public void executeShouldNotCreateAgentWhenMaximumAgentsExist() throws Exception {
        // Arrange
        agents.add(new Agent("id1", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id2", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id3", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        when(pluginRequest.listAgents()).thenReturn(agents);
        Map<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_MAX_INSTANCE_LIMIT, "3");
        createAgentRequest = new CreateAgentRequest("abc-key", properties, job1, "");
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), anyString(), any(PluginSettings.class),
                any(OpenstackClientWrapper.class), anyString(), anyBoolean())).thenReturn(true);
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper, pendingAgents);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, never()).create(any(CreateAgentRequest.class), any(PluginSettings.class), anyString());
    }

    @Test
    public void executeShouldNotThrowNumberFormatExceptionWhenLimitIsEmptyString() throws Exception {
        // Arrange
        when(pluginRequest.listAgents()).thenReturn(agents);
        Map<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_MAX_INSTANCE_LIMIT, "");
        createAgentRequest = new CreateAgentRequest("abc-key", properties, job1, "testing");
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), anyString(), any(PluginSettings.class),
                any(OpenstackClientWrapper.class), anyString(), anyBoolean())).thenReturn(true);
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper, pendingAgents);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, times(1)).create(any(CreateAgentRequest.class), any(PluginSettings.class), anyString());
    }

    @Test
    public void executeShouldNotCreateAgentWhenMoreThanMaximumAgentsExist() throws Exception {
        // Arrange
        agents.add(new Agent("id1", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id2", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id3", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id4", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id5", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id6", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        when(pluginRequest.listAgents()).thenReturn(agents);
        Map<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_MAX_INSTANCE_LIMIT, "3");
        createAgentRequest = new CreateAgentRequest("abc-key", properties, job1, "testing");
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), anyString(), any(PluginSettings.class),
                any(OpenstackClientWrapper.class), anyString(), anyBoolean())).thenReturn(true);
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper, pendingAgents);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, never()).create(any(CreateAgentRequest.class), any(PluginSettings.class), anyString());
    }

    @Test
    public void executeShouldNotCreateAgentWhenMoreThanMaximumAgentsExistUsingPluginDefaultValue() throws Exception {
        // Arrange
        agents.add(new Agent("id1", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id2", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id3", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id4", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id5", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id6", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id7", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id8", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id9", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id10", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id11", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        when(pluginRequest.listAgents()).thenReturn(agents);
        Map<String, String> properties = new HashMap<>();
        createAgentRequest = new CreateAgentRequest("abc-key", properties, job1, "testing");
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), anyString(), any(PluginSettings.class),
                any(OpenstackClientWrapper.class), anyString(), anyBoolean())).thenReturn(true);
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper, pendingAgents);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, never()).create(any(CreateAgentRequest.class), any(PluginSettings.class), anyString());
    }
}