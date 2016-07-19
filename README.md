# alma-toolkit
## Alma Toolkit

### Prerequisites
- JDK7
- Maven

### Build
To build we go into the project folder and use:
```
mvn -DskipTests package
```

This creates a ```target``` folder within which you will find package archives
for the application:
- alma-toolkit-x.x.x-dist.tar.gz
- alma-toolkit-x.x.x-dist.zip

### Deploy
Extract the contents of one of the archives into the folder you want to run it
from, using either tar or unzip as required.

### Run
To run the application:
```
java -jar alma-toolkit-x.x.x.jar [options]
```

## Tasks

### fetchReport

Task to fetch a report from Alma Analytics and save it as a CSV file


### fixUserIdentifiers

A Macquarie University specific task for correcting user identifiers for patrons.


### listUsers

Initial sample task to fetch user records from Alma.


### setUserPurgeDates

A task that takes a list of identifiers, an expiry date and a purge date and updates the provided identifers with those dates.


### updateResourcePartners (in development)

Updates Alma with resource sharing partners from LADD and Tepuna.
