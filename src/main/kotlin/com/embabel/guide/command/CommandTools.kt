package com.embabel.guide.command

import com.embabel.hub.PersonaService
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam

/**
 * Tool methods callable by the LLM during command execution (pass 2).
 * Created per-request with user context baked in, then registered via withToolObject().
 * All commands are executed via the frontend websocket round-trip.
 */
class CommandTools(
    private val webUserId: String?,
    private val personaService: PersonaService,
    private val commandExecutor: CommandExecutor,
) {

    private val logger = LoggerFactory.getLogger(CommandTools::class.java)

    @Tool(description = "Change the user's persona/character. Use this when the user wants to switch to a different persona.")
    fun changePersona(
        @ToolParam(description = "Name of the persona to switch to") name: String,
    ): String {
        val personas = personaService.listPersonasForUser(webUserId)
        val match = personas.find { it.name.equals(name, ignoreCase = true) }
            ?: return "Unknown persona '$name'. Available personas: ${personas.joinToString { it.name }}"

        return commandExecutor.executePersonaChange(match.id, webUserId)
    }
}
