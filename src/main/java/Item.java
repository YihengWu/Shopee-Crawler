import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.Connection.Response;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A product and all of its reviews
 *
 * @author Yiheng Wu
 * @date 2019/7/29
 */

public class Item {

    public String itemID;
    public String shopID;
    public ArrayList<UserReview> userReviews;
    public static int itemTotalCount = 0;


    public Item(String itemID, String shopID) {
        this.itemID = itemID;
        this.shopID = shopID;
        this.userReviews = new ArrayList<UserReview>();
    }

    public void addReview(UserReview userReview) {
        userReviews.add(userReview);
    }

    /**
     * retry the Jsoup connection
     * @param url
     */
    public static Response jsoup_load_with_retry(String url) throws IOException {
        int max_retry = 10;
        int retry = 1;
        int sleep_sec = 2;
        //org.jsoup.nodes.Document content = null;
        Response content = null;
        while(retry <= max_retry){
            try {
                content = Jsoup.connect(url).timeout(10 * 1000).ignoreContentType(true).execute();  //.get()返回的是html,document
                break;
            } catch (Exception ex){
                //wait before retry
                System.out.println(ex.getMessage() + " retrying..");
                try {
                    TimeUnit.SECONDS.sleep(sleep_sec);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            retry++;
        }
        return content;
    }

    /**
     * Fetch all reviews for the item from shopee.sg
     */
    public void fetchReview() {
        //先抓取一页获取总评论数
        int allReviewCount = 0;
        itemTotalCount++;
        try {
            //  返回所有评论的json
            String countJsonString = jsoup_load_with_retry("https://shopee.sg/api/v2/item/get_ratings?itemid=" + itemID + "&limit=5&offset=0" + "&shopid=" + shopID + "&type=0").body();
            JSONObject countJson = JSONObject.fromObject(countJsonString);
            allReviewCount = countJson.getJSONObject("data").getJSONObject("item_rating_summary").getInt("rating_total");
        }
        catch(Exception e){
            e.printStackTrace();
        }
        if (allReviewCount == 0) {
            return;
        }

        int highestOffset = allReviewCount/100*100;

        //再循环抓取每一条评论
        for (int j=0;j<=highestOffset;j+=100) {
            String url = "https://shopee.sg/api/v2/item/get_ratings?itemid=" + itemID + "&limit=100" + "&offset=" + j
                    + "&shopid=" + shopID + "&type=0";
            try {
                //  返回所有评论的json
                String reviewJsonString = jsoup_load_with_retry(url).body();
                JSONObject reviewJson = JSONObject.fromObject(reviewJsonString);
                JSONArray allReviews = reviewJson.getJSONObject("data").getJSONArray("ratings");
                int totalReviewCount = allReviews.size();

                if (totalReviewCount == 0) {
                    System.out.println(itemID + " has no review");
                } else {
                    for (int i = 0; i < totalReviewCount; i++) {
                        JSONObject review = allReviews.getJSONObject(i);
                        UserReview userReview = new UserReview(review.getString("author_username"), review.getInt("rating_star"), review.getString("comment"), review.getString("orderid"), review.getJSONArray("product_items").getJSONObject(0).getString("name"));
                        this.addReview(userReview);
                    }
                }
            } catch (Exception e) {
                System.out.println(itemID + " " + "Exception" + " " + e.toString());
            }
        }

    }

    /**
     * Write all reviews into a Sqlite database
     *
     * @param database Sqlite database file path
     */
    public synchronized void writeReviewsToDatabase(String database) throws InvalidKeyException, ClassNotFoundException, NoSuchAlgorithmException, ClientProtocolException, SQLException, IOException {
        DatabaseUpdater.doUpdate(database, userReviews, itemID, shopID);

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String timeNow = dateFormat.format(date);
        System.out.println(this.itemID + " Finished " + timeNow);

    }

    public static List<Item> searchKeyword(String keyword) {
        //newest是偏移量，经测试最大值是8000，而用户从html页面其实只能查询5000条商品
        //limit是每次返回的条数，最大值是100
        List<Item> Items = new ArrayList<Item>();

        for (int i=0; i<=8000; i+=100) {
            String searchURL = "https://shopee.sg/api/v2/search_items/?by=relevancy&keyword=" + keyword + "&limit=100&newest=" + i + "&order=desc&page_type=search";
            try {
                String searchJsonString = jsoup_load_with_retry(searchURL).body();
                JSONObject searchJson = JSONObject.fromObject(searchJsonString);
                JSONArray allSearch = searchJson.getJSONArray("items"); //应该返回<=100条
                for (int j=0; j<allSearch.size(); j++) {
                    JSONObject itemJSON = allSearch.getJSONObject(j);
                    Item newItem = new Item(itemJSON.getString("itemid"), itemJSON.getString("shopid"));
                    System.out.println("搜索获得 " + itemJSON.getString("name") + " 的信息");
                    Items.add(newItem);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return Items;
    }

    public static void main(String[] args) {
        List<Item> allItems = searchKeyword("huawei");

        System.out.println("搜索信息完毕");

        for (Item item: allItems) {
            //Item huaWei = new Item("1611921257", "998058"); //在shopee是上某个店铺中的某款华为手机
            //huaWei.fetchReview();
            System.out.println("正在抓取itemID=" + item.itemID + "的评论");
            item.fetchReview();
            try {
                item.writeReviewsToDatabase("D:\\CrawlerData\\test.db");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }


}

