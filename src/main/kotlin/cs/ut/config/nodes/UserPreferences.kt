package cs.ut.config.nodes

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlType

@XmlType(propOrder = ["enabled", "userName", "userGroup", "sudo", "acp", "authorized", "requirePassword"])
@XmlAccessorType(XmlAccessType.FIELD)
class UserPreferences(
    @XmlElement
    val enabled: Boolean,

    @XmlElement
    val userName: String,

    @XmlElement
    val userGroup: String,

    @XmlElement
    val sudo: String,

    @XmlElement
    val acp: String,

    @XmlElement
    val authorized: String,

    @XmlElement
    val requirePassword: Boolean
) {
    constructor() : this(false, "", "", "", "", "", true)
}