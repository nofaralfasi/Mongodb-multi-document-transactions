package com.mongodb;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.models.Cart;
import com.mongodb.models.Product;
import org.bson.BsonDocument;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.inc;
import static java.util.logging.Logger.getLogger;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class Transactions {

	private static MongoClient client;
	private static MongoCollection<Cart> cartCollection;
	private static MongoCollection<Product> productCollection;

	private final BigDecimal CHOCOLATE_PRICE = BigDecimal.valueOf(3);
	private final String CHOCOLATE_ID = "chocolate";

	private final Bson stockUpdate = inc("stock", -2);
	private final Bson filterId = eq("_id", CHOCOLATE_ID);
	private final Bson filterNofar = eq("_id", "Nofar");
	private final Bson matchChocolate = elemMatch("items", eq("productId", "chocolate"));
	private final Bson incrementChocolates = inc("items.$.quantity", 2);

	public static void main(String[] args) {
		System.out.println("Transactions");
		try {
			if (args.length > 0) {
				System.out.println("Connecting to server on port 27017 (localhost)");
				initMongoDB(args[0]);
			} else {
				System.out.println("Connecting to server on port 49045");
				initMongoDB("mongodb://admin:nofar101@ds349045.mlab.com:49045/mongo");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		new Transactions().demo();
	}

	private static void initMongoDB(String mongodbURI) {
		System.out.println("mongodbURI: " + mongodbURI + "\n");
		getLogger("org.mongodb.driver").setLevel(Level.SEVERE);
		CodecRegistry codecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
				fromProviders(PojoCodecProvider.builder().register("com.mongodb.models").build()));
		MongoClientOptions.Builder options = new MongoClientOptions.Builder().codecRegistry(codecRegistry);
		MongoClientURI uri = new MongoClientURI(mongodbURI, options);
		client = new MongoClient(uri);
		MongoDatabase db = client.getDatabase("mongo");
		cartCollection = db.getCollection("cart", Cart.class);
		productCollection = db.getCollection("product", Product.class);
	}

	private void demo() {
		clearCollections();
		insertProductChocolate();
		printDatabaseState();
		System.out.println("#########  NO  TRANSACTION #########");
		System.out.println("Nofar wants 2 chocolates.");
		System.out.println(
				"We have to create a cart in the 'cart' collection and update the stock in the 'product' collection.");
		System.out.println("The 2 actions are correlated but can not be executed on the same cluster time.");
		System.out.println(
				"Any error blocking one operation could result in stock error or a sale of chocolate that we can't fulfill as we have no stock.");
		System.out.println("---------------------------------------------------------------------------");
		ClientWantsChocolates("Nofar", 2);
		sleep();
		removingChocolatesFromStock(2);
		System.out.println("####################################\n");
		printDatabaseState();
		sleep();
		System.out.println("Now we can update the 2 collections simultaneously.");
		System.out.println("The 2 operations only happen when the transaction is committed.");
		System.out.println("---------------------------------------------------------------------------");
		System.out.println("\n######### WITH TRANSACTION #########");
		System.out.println("Now Alex wants 3 chocolates.");
		System.out.println("This time we do not have enough chocolates in stock so the transaction will rollback.");
		System.out.println("---------------------------------------------------------------------------");
		ClientWantsExtraChocolatesInTransactionThenCommitOrRollback("Alex",3);
		client.close();
	}

	private void ClientWantsExtraChocolatesInTransactionThenCommitOrRollback(String name, int amount) {
		ClientSession session = client.startSession();
		try {
			session.startTransaction(TransactionOptions.builder().writeConcern(WriteConcern.MAJORITY).build());
			ClientWantsExtraChocolates(session, name, amount);
			sleep();
			removingChocolateFromStock(session);
			session.commitTransaction();
		} catch (MongoCommandException e) {
			session.abortTransaction();
			System.out.println("####### ROLLBACK TRANSACTION #######");
		} finally {
			session.close();
			System.out.println("####################################\n");
			printDatabaseState();
		}
	}

	private void removingChocolatesFromStock(int amount) {
		System.out.println("Trying to update chocolate stock : -" + amount + " chocolates.");
		try {
			productCollection.updateOne(filterId, stockUpdate);
		} catch (MongoCommandException e) {
			System.out.println("#####   MongoCommandException  #####");
			System.out.println("##### STOCK CANNOT BE NEGATIVE #####");
			throw e;
		}
	}

	private void removingChocolateFromStock(ClientSession session) {
		System.out.println("Trying to update chocolate stock : -2 chocolates.");
		try {
			productCollection.updateOne(session, filterId, stockUpdate);
		} catch (MongoCommandException e) {
			System.out.println("#####   MongoCommandException  #####");
			System.out.println("##### STOCK CANNOT BE NEGATIVE #####");
			throw e;
		}
	}

	private void ClientWantsChocolates(String name, int amount) {
		System.out.println(name + " adds " + amount + " chocolates in his/her cart.");
		cartCollection.insertOne(
				new Cart(name, Collections.singletonList(new Cart.Item(CHOCOLATE_ID, amount, CHOCOLATE_PRICE))));
	}

	private void ClientWantsExtraChocolates(ClientSession session, String clientName, int amount) {
		System.out.println("Updating " + clientName + " cart : adding " + amount + " chocolates.");
		cartCollection.updateOne(session, and(filterNofar, matchChocolate), incrementChocolates);
	}

	private void insertProductChocolate() {
		productCollection.insertOne(new Product(CHOCOLATE_ID, 3, CHOCOLATE_PRICE));
	}

	private void clearCollections() {
		productCollection.deleteMany(new BsonDocument());
		cartCollection.deleteMany(new BsonDocument());
	}

	private void printDatabaseState() {
		System.out.println("Database state:");
		printProducts(productCollection.find().into(new ArrayList<>()));
		printCarts(cartCollection.find().into(new ArrayList<>()));
		System.out.println();
	}

	private void printProducts(List<Product> products) {
		products.forEach(System.out::println);
	}

	private void printCarts(List<Cart> carts) {
		if (carts.isEmpty())
			System.out.println("No carts...");
		else
			carts.forEach(System.out::println);
	}

	private void sleep() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			System.err.println("Error...");
			e.printStackTrace();
		}
	}
}
