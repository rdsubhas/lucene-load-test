* Download and extract lucene data under `data` folder here
* Run: `docker-compose build`
* Plain Lucene Disk-Lookup Test:
  * Run: `docker-compose scale local=<number of procs>`
* Remote Server Test:
  * First start the server: `docker-compose up -d server`
  * If you run the server in a separate machine, then edit `.env` and set the IP
  * There are two ways to test remote: Blocking (every network call blocks) and Async (network calls don't block)
  * Run the blocking remote lookup test: `docker-compose scale blocking=<# of procs>`
  * Run the async remote lookup test: `docker-compose scale async=<# of procs>`
