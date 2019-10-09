package GraphProcess;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;

public class GraphDatabase  implements AutoCloseable{
private  Driver driver;
public GraphDatabase(){

}
public GraphDatabase(String url,String username,String password){
    driver= org.neo4j.driver.v1.GraphDatabase.driver(url, AuthTokens.basic(username,password));

}
@Override
public void close() throws Exception {
        driver.close();
    }
//public static void addNode()

}
