package com.healthmine.testforge.template.entities

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable

@Embeddable
class FeatureEmployerGroupXrefId(
    @Column(name = "EMPLOYER_GROUP_ID")
    val employerGroupId: Long = 0,

    @Column(name = "FEATURE_ID")
    val featureId: String = ""
) : Serializable

@Entity
@Table(name = "FEATURE_EMPLOYER_GROUP_XREF", schema = "COM")
class FeatureEmployerGroupXref(
    @EmbeddedId
    val id: FeatureEmployerGroupXrefId,

    @Column(name = "IS_ENABLED")
    var isEnabled: Int = 1
)
