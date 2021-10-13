# Tower Agent

Tower Agent allows Tower to launch pipelines on HPC clusters that do not allow 
direct access throw an SSH client. 

Tower Agent it's a standalone process that when executed in a node that can submit 
jobs to the cluster (i.e. the login node) it establishes an authenticated secure 
reverse connection with Tower, allowing Tower to submit and monitor new jobs. The 
jobs are submitted on behalf of the same user that it's running the agent process.

## Build binary version

```
/gradlew nativeBuild
```


