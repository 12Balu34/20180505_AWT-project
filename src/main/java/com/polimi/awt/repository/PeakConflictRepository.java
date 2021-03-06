package com.polimi.awt.repository;

import com.polimi.awt.model.PeakConflict;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PeakConflictRepository {

    @PersistenceContext
    private EntityManager entitiyManager;

    public List<PeakConflict> findPeakConflictsByCampaignId(Long campaignId) {

        List<Object[]> resultListHelper = entitiyManager.createNativeQuery("select p.id peakId, p.name peakName, " +
                "sum(is_valid) numberOfAnnotationsWithValidPeak, sum(!is_valid) numberOfAnnotationsWithInvalidPeak " +
                "from peak p join annotation a on p.id = a.peak_id " +
                "where p.campaign_id = :campaignId " +
                "group by peak_id " +
                "having count(*) > 1 and min(is_valid) = 0 and max(is_valid) = 1 ##List of Peaks with conflicts").setParameter("campaignId", campaignId).getResultList();

        List<PeakConflict> resultList = new ArrayList<>();
        for (Object[] currentObject : resultListHelper
                ) {
            BigInteger peakId = (BigInteger) currentObject[0];
            String peakName = (String) currentObject[1];
            BigDecimal numberOfAnnotationsWithValidPeak = (BigDecimal) currentObject[2];
            BigDecimal numberOfAnnotationsWithInvalidPeak = (BigDecimal) currentObject[3];

            resultList.add(
                    new PeakConflict(
                            peakId.longValue(),
                            peakName,
                            numberOfAnnotationsWithValidPeak.intValue(),
                            numberOfAnnotationsWithInvalidPeak.intValue()
                    )
            );
        }
        return resultList;
    }

}
