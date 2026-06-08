package org.example.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "system_settings")
public class SystemSettings {

    @Id
    private String id;

    @Field("company_name")
    private String companyName;

    @Field("support_email")
    private String supportEmail;

    @Field("allow_registration")
    private boolean allowRegistration;

    @Field("maintenance_mode")
    private boolean maintenanceMode;

    @Field("max_upload_size_mb")
    private int maxUploadSizeMB;

    // A static constant ID we can use to ensure we only ever have 1 settings document
    public static final String SINGLETON_ID = "singleton_system_settings";
}
