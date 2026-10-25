package com.livraison.dto;

public class AssignerRequest {

    private Long livreurId;

    public AssignerRequest() {
    }

    public AssignerRequest(Long livreurId) {
        this.livreurId = livreurId;
    }

    public Long getLivreurId() {
        return livreurId;
    }

    public void setLivreurId(Long livreurId) {
        this.livreurId = livreurId;
    }
}
