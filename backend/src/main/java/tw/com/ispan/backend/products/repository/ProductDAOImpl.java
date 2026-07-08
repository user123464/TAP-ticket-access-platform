package tw.com.ispan.backend.products.repository;

import java.util.ArrayList;
import java.util.List;

import org.cloudinary.json.JSONObject;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import tw.com.ispan.backend.products.entity.Product;

@Repository
public class ProductDAOImpl implements ProductDAO {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public long count(JSONObject obj) {
        Integer productId = obj.isNull("productId") ? null : obj.getInt("productId");
        String productName = obj.isNull("productName") ? null : obj.getString("productName");
        Double unitPriceMin = obj.isNull("unitPriceMin") ? null : obj.getDouble("unitPriceMin");
        Double unitPriceMax = obj.isNull("unitPriceMax") ? null : obj.getDouble("unitPriceMax");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);

        // from product
        Root<Product> table = criteriaQuery.from(Product.class);

        // where
        List<Predicate> predicates = new ArrayList<>();
        if (productId != null) {
            predicates.add(criteriaBuilder.equal(table.get("productId"), productId));
        }
        if (productName != null && productName.length() != 0) {
            predicates.add(criteriaBuilder.like(table.get("productName"), "%" + productName + "%"));
        }
        if (unitPriceMin != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(table.get("unitPrice"), unitPriceMin));
        }
        if (unitPriceMax != null) {
            predicates.add(criteriaBuilder.lessThan(table.get("unitPrice"), unitPriceMax));
        }

        if (!predicates.isEmpty()) {
            criteriaQuery = criteriaQuery.where(predicates.toArray(new Predicate[predicates.size()]));
        }

        // select count(*)
        criteriaQuery = criteriaQuery.select(criteriaBuilder.count(table));

        TypedQuery<Long> typedQuery = entityManager.createQuery(criteriaQuery);
        Long result = typedQuery.getSingleResult();
        if (result != null) {
            return result.longValue();
        } else {
            return 0L;
        }
    }

    @Override
    public List<Product> find(JSONObject obj) {
        Integer productId = obj.isNull("productId") ? null : obj.getInt("productId");
        String productName = obj.isNull("productName") ? null : obj.getString("productName");
        Double unitPriceMin = obj.isNull("unitPriceMin") ? null : obj.getDouble("unitPriceMin");
        Double unitPricemax = obj.isNull("unitPricemax") ? null : obj.getDouble("unitPricemax");

        int start = obj.isNull("start") ? 0 : obj.getInt("start");
        int rows = obj.isNull("rows") ? 3 : obj.getInt("rows");
        String order = obj.isNull("order") ? "productId" : obj.getString("order");
        boolean dir = obj.isNull("dir") ? false : obj.getBoolean("dir");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> criteriaQuery = criteriaBuilder.createQuery(Product.class);

        // from product
        Root<Product> table = criteriaQuery.from(Product.class);

        // where
        List<Predicate> predicates = new ArrayList<>();
        if (productId != null) {
            predicates.add(criteriaBuilder.equal(table.get("productId"), productId));
        }
        if (productName != null && productName.length() != 0) {
            predicates.add(criteriaBuilder.like(table.get("productName"), "%" + productName + "%"));
        }
        if (unitPriceMin != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(table.get("unitPrice"), unitPriceMin));
        }
        if (unitPricemax != null) {
            predicates.add(criteriaBuilder.lessThan(table.get("unitPrice"), unitPricemax));
        }

        if (!predicates.isEmpty()) {
            criteriaQuery = criteriaQuery.where(predicates.toArray(new Predicate[predicates.size()]));
        }

        // order by
        if (dir) {
            criteriaQuery = criteriaQuery.orderBy(
                    criteriaBuilder.desc(table.get(order)));
        } else {
            criteriaQuery = criteriaQuery.orderBy(
                    criteriaBuilder.asc(table.get(order)));
        }

        TypedQuery<Product> typedQuery = entityManager.createQuery(criteriaQuery)
                .setFirstResult(start)
                .setMaxResults(rows);

        List<Product> results = typedQuery.getResultList();
        if (results != null && !results.isEmpty()) {
            return results;
        } else {
            return null;
        }
    }

}
