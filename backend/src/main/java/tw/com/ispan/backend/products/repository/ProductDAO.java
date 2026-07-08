package tw.com.ispan.backend.products.repository;

import java.util.List;
import org.cloudinary.json.JSONObject;

import tw.com.ispan.backend.products.entity.Product;

public interface ProductDAO {

    List<Product> find(JSONObject obj);

    long count(JSONObject obj);
}
