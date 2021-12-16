## Tower Agent

Tower Agent allows Tower to launch pipelines on HPC clusters that do not allow direct access throw an SSH client.

Tower Agent it's a standalone process that when executed in a node that can submit jobs to the cluster (i.e. the login node) it establishes an authenticated secure reverse connection with Tower, allowing Tower to submit and monitor new
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
- You need to create a **personal access token** at Tower. See [here](https://help.tower.nf/api/overview/#authentication).
- At Tower you need to add new **Tower Agent** credentials in a workspace. See [here](https://help.tower.nf/credentials/overview/).
- When you create the credentials you'll get an **Agent Connection ID**. You can use that one or just set your own, the important is that you use the same connection ID at the workspace credentials and when you run the agent.

Running the Agent:

- The agent has to be always running to accept incoming requests from Tower. For this we recommend that you run it using a terminal multiplexer like [tmux](https://github.com/tmux/tmux) or [GNU Screen](https://www.gnu.org/software/screen/), so that it keeps running even if you close the SSH session.
```
export TOWER_ACCESS_TOKEN=<YOUR TOKEN>
./tw-agent <your agent connection ID>
```

### Tips

- If you are using an on premises Tower you can set the API url using `TOWER_API_ENDPOINT` environment variable or the `--url` option.
- By default, the Agent uses the folder `~/work` at your home as working directory. You can change it using the `--work-dir` option.
- The work directory must exist before running the agent.
- You can also change the work directory at Tower when you create a compute environment or pipeline.

### Usage
```

Usage: tw-agent [OPTIONS] AGENT_CONNECTION_ID

Nextflow Tower Agent

Parameters:
*     AGENT_CONNECTION_ID    Agent connection ID to identify this agent.

Options:
* -t, --access-token=<token> Tower personal access token. If not provided TOWER_ACCESS_TOKEN variable will be used.
  -u, --url=<url>            Tower server API endpoint URL. If not provided TOWER_API_ENDPOINT variable will be used [default: https://api.tower.nf].
  -w, --work-dir=<workDir>   Default path where the pipeline scratch data is stored. It can be changed when launching a pipeline from Tower [default: ~/work].
  -h, --help                 Show this help message and exit.
  -V, --version              Print version information and exit.

```
