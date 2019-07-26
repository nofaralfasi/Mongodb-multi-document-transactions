# Mongodb-multi-document-transactions

https://docs.mongodb.com/manual/core/transactions/

## Project's Database:  

The Cart Collection:
![compass-cart](https://user-images.githubusercontent.com/38184193/53634003-b92f8f80-3c21-11e9-8ee7-069ac60b4428.png)

The Product Collection:
![compass-product](https://user-images.githubusercontent.com/38184193/53634179-2d6a3300-3c22-11e9-9b7b-ced392845ec8.png)

### Step 1: Start MongoDB
Start a single node MongoDB ReplicaSet in version 4.0.0 minimum on localhost, port 27017.

#### Using Docker:
You can use start-mongo.sh.
When you are done, you can use stop-mongo.sh.
If you want to connect to MongoDB with the Mongo Shell, you can use connect-mongo.sh.

### Step 2: Start Java
This demo contains two main programs: ChangeStreams.java and Transactions.java.

Change Steams allow you to be notified of any data changes within a MongoDB collection or database.
The Transaction process is the demo itself.
You need two shells to run them.

First shell:
./compile-docker.sh
./change-streams-docker.sh

Second shell:
./transactions-docker.sh


MongoDB’s transactions are a conversational set of related operations that must atomically commit or fully rollback with all-or-nothing execution.

Transactions are used to make sure operations are atomic even across multiple collections or databases. Thus, with snapshot isolation reads, another user can only see all the operations or none of them.

