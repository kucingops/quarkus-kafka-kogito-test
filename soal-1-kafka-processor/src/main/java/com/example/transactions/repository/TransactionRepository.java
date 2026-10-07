package com.example.transactions.repository;

import java.util.List;

import com.example.transactions.entity.TransactionEntity;
import com.example.transactions.model.AmountCategory;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TransactionRepository implements PanacheRepositoryBase<TransactionEntity, String> {

    public List<TransactionEntity> findLatest(AmountCategory category, Boolean highRisk) {
        Sort sort = Sort.descending("processedAt");
        if (category != null && highRisk != null) {
            return list("category = ?1 and highRisk = ?2", sort, category, highRisk);
        }
        if (category != null) {
            return list("category", sort, category);
        }
        if (highRisk != null) {
            return list("highRisk", sort, highRisk);
        }
        return listAll(sort);
    }

    public List<CategorySummary> summarizeByCategory() {
        return getEntityManager()
                .createQuery("select new com.example.transactions.repository.CategorySummary("
                        + "t.category, count(t), sum(t.amountIdr)) "
                        + "from TransactionEntity t group by t.category order by t.category",
                        CategorySummary.class)
                .getResultList();
    }
}
