package main.java.org.bigdata.hbase.spring;

import java.io.IOException;

import javax.security.auth.login.Configuration;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.BasicConfigurator;

import main.java.org.bigdata.hbase.spring.thrift.User;
import main.java.org.bigdata.hbase.spring.util.SerializationUtil;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.hadoop.hbase.HbaseTemplate;
import org.springframework.data.hadoop.hbase.TableCallback;

/**
 * Creates connection to HBase local cluster and create a table if not existed
 * 
 * @author ghstreddy
 * 
 */
public class HBaseConnection {

    private final String tableName = "spring_table";

    public HbaseTemplate getHbaseTemplate() {
        
        String workingDir = System.getProperty("user.dir");
        System.out.println("Current working directory : " + workingDir);
        
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "hbase-beans.xml");
        HbaseTemplate template = (HbaseTemplate) context.getBean("htemplate");

        // Add by Aaron.Z 2014.9.20
        if (context.isRunning()) {
            //context.close();
        }

        return template;
    }

    public static void main(String[] args) throws IOException {
        // Add by Aaron.Z
        BasicConfigurator.configure(); // log4j config
        
        HBaseConnection connection = new HBaseConnection();
        HbaseTemplate hbaseTemplate = connection.getHbaseTemplate();
        
        // Add by Aaron.Z
        org.apache.hadoop.conf.Configuration configuration = hbaseTemplate.getConfiguration();
        HBaseAdmin admin = new HBaseAdmin(configuration);
        connection.createTable(admin);
        connection.putData(hbaseTemplate);
        User u = connection.getData(hbaseTemplate);
        System.out.println("User : " + u);
        admin.close();
    }

    /**
     * Read data from HBase table
     */
    private User getData(HbaseTemplate hbaseTemplate) {
        return hbaseTemplate.execute(tableName, new TableCallback<User>() {

            @Override
            public User doInTable(HTableInterface table) throws Throwable {
                Get get = new Get(Bytes.toBytes("THIRUPATHI REDDY"));
                Result result = table.get(get);
                byte[] value = result.getValue(Bytes.toBytes("cf1"),
                        Bytes.toBytes("cf1"));
                User user = new User();
                SerializationUtil.deserialize(value, user);
                return user;
            }
        });

    }

    /**
     * Write data into HBase table
     */
    private void putData(HbaseTemplate hbaseTemplate) {
        hbaseTemplate.execute(tableName, new TableCallback<User>() {

            public User doInTable(HTableInterface table) throws Throwable {

                User user = new User();
                user.setFirstName("THIRUPATHI REDDY");
                user.setLastName("GUDURU");
                user.setEmail("email@email.com");
                user.setAddress("OVERLAND PARK,KS");
                // create a put with row key
                Put p = new Put(Bytes.toBytes(user.getFirstName()));
                // add row value
                p.add(Bytes.toBytes("cf1"), Bytes.toBytes("cf1"),
                        SerializationUtil.serialize(user));
                table.put(p);
                return user;
            }
        });

    }

    public void createTable(HBaseAdmin admin) throws IOException {
        if (!admin.tableExists(tableName)) {
            HTableDescriptor descriptor = new HTableDescriptor(
                    Bytes.toBytes(tableName));
            HColumnDescriptor column = new HColumnDescriptor(
                    Bytes.toBytes("cf1"));
            descriptor.addFamily(column);
            admin.createTable(descriptor);
            System.out.println("Table Created");
        } else
            System.out.println("Table Already Created");
    }
}
