@echo off
:: Check environment variables
echo Checking environment variables...
set OPENAI_KEY_MISSING=false
set ANTHROPIC_KEY_MISSING=false
set OLLAMA_AVAILABLE=false
set OLLAMA_BASE_URL=%SPRING_AI_OLLAMA_BASE_URL%
if "%OLLAMA_BASE_URL%"=="" set OLLAMA_BASE_URL=http://localhost:11434

if "%OPENAI_API_KEY%"=="" (
    echo OPENAI_API_KEY environment variable is not set
    echo OpenAI models will not be available
    echo Get an API key at https://platform.openai.com/api-keys
    echo Please set it with: set OPENAI_API_KEY=your_api_key
    set OPENAI_KEY_MISSING=true
) else (
    echo OPENAI_API_KEY set: OpenAI models are available
)

if "%ANTHROPIC_API_KEY%"=="" (
    echo ANTHROPIC_API_KEY environment variable is not set
    echo Claude models will not be available
    echo Get an API key at https://www.anthropic.com/api
    echo Please set it with: set ANTHROPIC_API_KEY=your_api_key
    set ANTHROPIC_KEY_MISSING=true
) else (
    echo ANTHROPIC_API_KEY set: Claude models are available
)

curl -fsS --max-time 3 "%OLLAMA_BASE_URL%/api/tags" >nul 2>&1
if "%ERRORLEVEL%"=="0" (
    echo Ollama is reachable at %OLLAMA_BASE_URL%
    set OLLAMA_AVAILABLE=true
) else (
    echo Ollama is not reachable at %OLLAMA_BASE_URL%
)

if "%OPENAI_KEY_MISSING%"=="true" if "%ANTHROPIC_KEY_MISSING%"=="true" if "%OLLAMA_AVAILABLE%"=="false" (
    echo ERROR: OpenAI, Anthropic, and Ollama are all unavailable.
    echo Configure at least one supported model provider before running the application.
    exit /b 1
)