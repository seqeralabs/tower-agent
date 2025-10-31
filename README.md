## Tower Agent

Tower Agent allows Tower to launch pipelines on HPC clusters that do not allow direct access through an SSH client.

Tower Agent is a standalone process that when executed in a node that can submit jobs to the cluster (i.e. the login node) it establishes an authenticated secure reverse connection with Tower, allowing Tower to submit and monitor new
jobs. The jobs are submitted on behalf of the same user that it's running the agent process.

### Installation

Tower Agent it is distributed as a single binary executable file. You only need to download it and execute it.

1. Download the latest Agent binary from Github: 
```
curl -fSL https://github.com/seqeralabs/tower-agent/releases/latest/download/tw-agent-linux-x86_64 > tw-agent
```

2. Make it executable:
```
chmod +x ./tw-agent
```

3. (OPTIONAL) Move it into a folder that is in your path.

### Quick start

Before running the Agent:
- You need to create a **personal access token** in Tower. See [here](https://docs.seqera.io/platform-cloud/api/overview#authentication).
- On the Tower side, you need to add new **Tower Agent** credentials in a workspace. See [here](https://docs.seqera.io/platform-cloud/credentials/agent_credentials).
- When you create the credentials you'll get an **Agent Connection ID**. You can use that one or just set your own, the important thing is that you use the same connection ID at the workspace credentials and when you run the agent.

Running the Agent:

- The agent has to be always running to accept incoming requests from Tower. For this we recommend that you run it using a terminal multiplexer like [tmux](https://github.com/tmux/tmux) or [GNU Screen](https://www.gnu.org/software/screen/), so that it keeps running even if you close the SSH session.
```
export TOWER_ACCESS_TOKEN=<YOUR TOKEN>
./tw-agent <your agent connection ID>
```

### Tips

- If you are using the agent with Tower Enterprise (on-prem) you can set the API url using `TOWER_API_ENDPOINT` environment variable or the `--url` option.
- By default, the Agent uses the folder `~/work` at your home as working directory. You can change it using the `--work-dir` option.
- The work directory **must** exist before running the agent.
- You can also change the work directory at Tower when you create a compute environment or pipeline.

### Usage
```

Usage: tw-agent [OPTIONS] AGENT_CONNECTION_ID

Nextflow Tower Agent

Parameters:
*     AGENT_CONNECTION_ID    Agent connection ID to identify this agent.

Options:
* -t, --access-token=<token> Tower personal access token. If not provided TOWER_ACCESS_TOKEN variable will be used.
  -u, --url=<url>            Tower server API endpoint URL. If not provided TOWER_API_ENDPOINT variable will be used [default: https://api.cloud.seqera.io].
  -w, --work-dir=<workDir>   Default path where the pipeline scratch data is stored. It can be changed when launching a pipeline from Tower [default: ~/work].
  -h, --help                 Show this help message and exit.
  -V, --version              Print version information and exit.

```

### Debugging with Trace Logging

To troubleshoot Agent connection or execution issues, you can enable trace-level logging using the `LOGGER_LEVELS_IO_SEQERA_TOWER_AGENT` environment variable:

```
export TOWER_ACCESS_TOKEN=<your-token>
export LOGGER_LEVELS_IO_SEQERA_TOWER_AGENT=TRACE

./tw-agent <connection-id> --work-dir=<path>
```

Or as a one-liner:
```
LOGGER_LEVELS_IO_SEQERA_TOWER_AGENT=TRACE ./tw-agent my-connection --work-dir ~/work
```

With trace logging enabled, you'll see detailed information about:
- WebSocket connection establishment and handshake
- All message exchanges (CommandRequest, CommandResponse, HeartbeatMessage, InfoMessage)
- Session expiration and reconnection attempts
- Command execution details (ProcessBuilder calls, exit codes)
- Full stack traces for errors
