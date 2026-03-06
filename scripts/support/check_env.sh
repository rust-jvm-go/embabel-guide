# Check environment variables

echo "Checking environment variables..."

OPENAI_KEY_MISSING=false
ANTHROPIC_KEY_MISSING=false
OLLAMA_AVAILABLE=false
OLLAMA_CHECK_SKIPPED=false
OLLAMA_BASE_URL="${SPRING_AI_OLLAMA_BASE_URL:-http://localhost:11434}"

if [ -z "${OPENAI_API_KEY}" ]; then
    echo "OPENAI_API_KEY environment variable is not set"
    echo "OpenAI models will not be available"
    echo "Get an API key at https://platform.openai.com/api-keys"
    echo "Please set it with: export OPENAI_API_KEY=your_api_key"
    OPENAI_KEY_MISSING=true
else
    echo "OPENAI_API_KEY set: OpenAI models are available"
fi

if [ -z "${ANTHROPIC_API_KEY}" ]; then
    echo "ANTHROPIC_API_KEY environment variable is not set"
    echo "Claude models will not be available"
    echo "Get an API key at https://www.anthropic.com/api"
    echo "Please set it with: export ANTHROPIC_API_KEY=your_api_key"
    ANTHROPIC_KEY_MISSING=true
else
    echo "ANTHROPIC_API_KEY set: Claude models are available"
fi

if command -v curl >/dev/null 2>&1; then
    if curl -fsS --max-time 3 "${OLLAMA_BASE_URL%/}/api/tags" >/dev/null 2>&1; then
        echo "Ollama is reachable at ${OLLAMA_BASE_URL}"
        OLLAMA_AVAILABLE=true
    else
        echo "Ollama is not reachable at ${OLLAMA_BASE_URL}"
    fi
else
    echo "curl is not installed; skipping Ollama reachability check"
    OLLAMA_CHECK_SKIPPED=true
fi

if [ "$OPENAI_KEY_MISSING" = true ] && [ "$ANTHROPIC_KEY_MISSING" = true ] && [ "$OLLAMA_AVAILABLE" = false ] && [ "$OLLAMA_CHECK_SKIPPED" = false ]; then
    echo "ERROR: OpenAI, Anthropic, and Ollama are all unavailable."
    echo "Configure at least one supported model provider before running the application."
    exit 1
fi
