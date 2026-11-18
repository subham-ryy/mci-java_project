package com.mci.agents;

import com.mci.core.*;
import com.mci.gateway.ToolGateway;
import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * Routes subtasks to the appropriate dedicated agent.
 */
public class AgentDispatcher implements Agent {

    private final NavigatorAgent navigatorAgent;
    private final DataFileAgent dataFileAgent;
    private final DockerAgent dockerAgent;
    private final ReportAgent reportAgent;
    private final SimpleAgent fallbackAgent;

    public AgentDispatcher(ChatLanguageModel model, ToolGateway gateway) {
        this.navigatorAgent = new NavigatorAgent(model, gateway);
        this.dataFileAgent = new DataFileAgent(model, gateway);
        this.dockerAgent = new DockerAgent(model, gateway);
        this.reportAgent = new ReportAgent(model, gateway);
        this.fallbackAgent = new SimpleAgent(model, gateway);
    }

    @Override
    public AgentResult execute(SubTask subTask) {
        return switch (subTask.agentType()) {
            case NAVIGATOR -> navigatorAgent.execute(subTask);
            case DATA_FILE -> dataFileAgent.execute(subTask);
            case DOCKER -> dockerAgent.execute(subTask);
            case REPORT -> reportAgent.execute(subTask);
            default -> fallbackAgent.execute(subTask);
        };
    }
}
