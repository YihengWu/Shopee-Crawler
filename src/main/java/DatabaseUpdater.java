import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.sql.*;

/**
 * Update or insert all reviews and product relating information to a SQLite database
 *
 * @author Yiheng Wu
 * @date 2019/7/29
 */
public class DatabaseUpdater {

    private static final Lock lock = new ReentrantLock();

    /**
     * @param database
     *            file name of the SQLite database
     * @param reviews
     *            an ArrayList of reviews
     * @param itemID
     *            ID of the item
     */
    public static void doUpdate(String database, ArrayList<UserReview> reviews, String itemID, String shopID) throws SQLException, ClassNotFoundException, IOException {
        lock.lock();
        try {
            Class.forName("org.sqlite.JDBC");
            // if database not exist
            if (!(new File(database).isFile())) {
                Statement stmt = null;
                try {
                    Connection conn = DriverManager
                            .getConnection("jdbc:sqlite:" + database);
                    stmt = conn.createStatement();
                    String sql = "CREATE TABLE review ( [KEY] INTEGER PRIMARY KEY, itemID TEXT, shopID TEXT, orderID TEXT unique, product_name TEXT, user_name TEXT, rating_star NUMERIC, comment TEXT);";
                    stmt.executeUpdate(sql);
                    stmt.close();
                    conn.close();
                } catch (Exception e) {
                    System.err.println(e.getClass().getName() + ": "
                            + e.getMessage());
                    System.exit(0);
                }
                System.out.println("Table created successfully");
            }

            Connection conn = DriverManager.getConnection("jdbc:sqlite:"
                    + database);
            PreparedStatement insertReview = conn
                    .prepareStatement("insert into review (itemID, shopID, orderID, product_name, user_name, rating_star, comment) values (?1, ?2, ?3, ?4, ?5, ?6, ?7);");

            for (UserReview review : reviews) {
                insertReview.setString(1, itemID);
                insertReview.setString(2, shopID);
                insertReview.setString(3, review.getOrderID());
                insertReview.setString(4, review.getProductName());
                insertReview.setString(5, review.getUserName());
                insertReview.setInt(6, review.getRatingStar());
                insertReview.setString(7, review.getComment());
                insertReview.addBatch();
            }
            conn.setAutoCommit(false);
            try {
                insertReview.executeBatch();
            } catch (SQLException e) {
                System.out.println("此次爬取有重复订单号（orderID）的数据插入数据库");
            }
            conn.commit();
            conn.close();
        } finally {
            lock.unlock();
        }
    }
}
