package com.decidir.sdk.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

/**
 * Created by biandra on 06/10/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundSubPaymentDescription implements Serializable {

    private Long id;
    private List<RefundDescription> history;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<RefundDescription> getHistory() {
        return history;
    }

    public void setHistory(List<RefundDescription> history) {
        this.history = history;
    }
}
