# brXM Lucene Index Exporter Application
### A standalone application that takes care of exporting [lucene index](https://documentation.bloomreach.com/library/enterprise/enterprise-features/lucene-index-export/lucene-index-export.html) from a running brXM cms instance, in a kubernetes cluster

This applicaiton pulls the lucene index from ${LIE_URL} (e.g. http://localhost:8080/cms/ws/indexexport) and saves it into path at ${LIE_LIVE_INDEX_DIR}. 
This path can be an NFS mount for example and can be mounted into brXM pods. 
That way, brXM pods can use this lucene index export to start faster

#### Run it locally in minikube:

(Tested minikube version: v1.5.2)

Install virtualbox https://www.virtualbox.org/wiki/Downloads

Install minikube https://github.com/kubernetes/minikube
```bash
brew cask install minikube
```
Start minikube with some additional resources

```bash
minikube --memory 8192 --cpus 2 start
```

Setup helm (tested with v3.0.0) (kubernetes package manager) https://github.com/helm/helm
```bash
brew install kubernetes-helm
```

Switch to kubernetes folder
```bash
cd kubernetes
```

Setup a postgresql db for brxm

```bash
./setup_db.sh
```

After db is up, create a brxm deployment (from kubernetes directory)

```bash
./deploy-brxm.sh
```

To be able to work with the docker daemon on your mac/linux host use the docker-env command in your shell
```bash
eval $(minikube docker-env)
```
* More info on the above command is at: https://kubernetes.io/docs/setup/minikube#reusing-the-docker-daemon

Now that you have run the eval command above, build the brxm-lucene-index-exporter image: (you have to keep using the same shell!)

```bash
cd .. # switch to pom.xml directory
mvn clean compile jib:dockerBuild
``` 

Create either a cronjob (lie-cronjob.yaml), job (lie-job.yaml)

```bash
kubectl create -f kubernetes/lie-cronjob.yaml
```

Remarks:
* Ideally you want to run the cronjob every 4 hours.
* The app also backs up the exported indexes by date (YYYYmmdd format), and deletes backed-up indexes that are older than
 ${LIE_INDEX_RETENTION_DAYS}. These backups are necessary when you restore a db backup 
 (You'd have to find a suitable export for that backup, you can't use an export of today for a backup taken 3 days ago).
* The directory in which the usable index is stored is at ${LIE_LIVE_INDEX_DIR}. 
Backed-up indexes are at ${LIE_BACKUP_INDEX_DIR}. ${LIE_TEMP_INDEX_DIR} is used for a temporary place to keep an index that is being downloaded.