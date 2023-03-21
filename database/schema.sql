CREATE TABLE IF NOT EXISTS `actor_communication_details` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_id` bigint NOT NULL,
  `actor_type` varchar(50) NOT NULL,
  `mobile_number` varchar(15) NOT NULL,
  `language_id` tinyint DEFAULT NULL,
  `active` boolean NOT NULL DEFAULT true,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `actor_communication_details_actor_index` UNIQUE (`actor_id`, `actor_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `actor_app_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_id` bigint NOT NULL,
  `actor_type` varchar(50) NOT NULL,
  `mobile_app_details_id` tinyint NOT NULL,
  `fcm_token` varchar(1024) NOT NULL,
  `active` boolean NOT NULL DEFAULT true,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `actor_app_token_index` UNIQUE (`actor_id`, `actor_type`, `mobile_app_details_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `templates` (
  `id` smallint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `language_id` tinyint NOT NULL,
  `content_type` ENUM('STRING', 'HTML') NOT NULL,
  `content` text NOT NULL,
  `title` varchar(512) DEFAULT NULL,
  `attributes` text,
  `active` boolean DEFAULT '1',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `template_name_language_index` UNIQUE (`name`, `language_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `languages` (
  `id` tinyint NOT NULL AUTO_INCREMENT,
  `code` varchar(10) NOT NULL,
  `name` varchar(20) NOT NULL,
  `unicode` boolean DEFAULT true,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `languages_code_index` UNIQUE (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `mobile_app_details` (
  `id` tinyint NOT NULL AUTO_INCREMENT,
  `app_id` varchar(128) DEFAULT NULL,
  `app_name` varchar(20) NOT NULL,
  `app_type` ENUM('ANDROID', 'IOS') NOT NULL,
  `fcm_api_key` varchar(1024) DEFAULT NULL,
  `afs_app_id` tinyint NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `mobile_app_details_app_id_index` UNIQUE (`app_id`),
  CONSTRAINT `mobile_app_details_afs_app_id_index` UNIQUE (`afs_app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `message_acknowledgements` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `actor_id` bigint(20) NOT NULL,
  `actor_type` varchar(30) NOT NULL,
  `communication_channel` varchar(16) NOT NULL,
  `reference_id` varchar(36) DEFAULT NULL,
  `template_name` varchar(64) DEFAULT NULL,
  `language_id` tinyint DEFAULT NULL,
  `message_content` text NOT NULL,
  `is_unicode` boolean DEFAULT NULL,
  `vendor_name` varchar(16) DEFAULT NULL,
  `vendor_message_id` varchar(128) DEFAULT '',
  `state` varchar(24) NOT NULL,
  `retry_count` int NOT NULL,
  `placeholders` text,
  `attributes` text,
  `vendor_delivery_time` timestamp NULL DEFAULT NULL,
  `actor_delivery_time` timestamp NULL DEFAULT NULL,
  `contact_type` varchar(45) DEFAULT NULL,
  `actor_contact_id` varchar(512) DEFAULT NULL,
  `parent_reference_id` varchar(45) DEFAULT NULL,
  `campaign_name` varchar(45) DEFAULT NULL,
  `version` bigint DEFAULT 1,
  `created_date` date not null,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`, `created_date`),
  UNIQUE KEY `message_acknowledgements_index` (`reference_id`,`vendor_message_id`,`communication_channel`,`created_date`),
  KEY `updated_message_time_idx` (`actor_id`,`actor_type`,`updated_at`),
  KEY `vendor_idx` (`vendor_message_id`,`vendor_name`),
  KEY `actor_contact_id_idx` (`actor_contact_id`,`created_at`),
  KEY `created_at_idx` (`created_at`)
)
PARTITION BY LIST COLUMNS(created_date)(
    PARTITION p20210726 VALUES IN ('2021-07-26'),
    PARTITION p20210727 VALUES IN ('2021-07-27'),
    PARTITION p20210728 VALUES IN ('2021-07-28'),
    PARTITION p20210729 VALUES IN ('2021-07-29'),
    PARTITION p20210730 VALUES IN ('2021-07-30'),
    PARTITION p20210731 VALUES IN ('2021-07-31'),
    PARTITION p20210725 VALUES IN ('2021-07-25')
    );

CREATE TABLE IF NOT EXISTS `actor_attributes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_id` bigint NOT NULL,
  `actor_type` VARCHAR(50) NOT NULL,
  `namespace` ENUM('NURTURE_FARM', 'NURTURE_SUSTAIN') NOT NULL,
  `attr_key` VARCHAR(512) NOT NULL,
  `attr_value` text NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `actor_attributes_index` UNIQUE (`actor_id`, `actor_type`, `attr_key`, `namespace`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `whatsapp_users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `mobile_number` varchar(15) NOT NULL,
  `status` enum('OPT_IN','OPT_OUT') NOT NULL,
  `opt_out_consent_sent` tinyint(1) DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `whatsapp_users_index` (`mobile_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `communication_engine`.`mobile_app_details`
DROP INDEX `mobile_app_details_app_id_index` ,
ADD INDEX `mobile_app_details_app_id_index` (`app_id` ASC, `app_type` ASC);

CREATE INDEX actor_mobile_details_index ON actor_communication_details(mobile_number, actor_type, active);

ALTER TABLE `communication_engine`.`templates`
ADD COLUMN `title` VARCHAR(45) NULL AFTER `content`;

ALTER TABLE `communication_engine`.`whatsapp_users`
ADD COLUMN `namespace` VARCHAR(16) NOT NULL AFTER `id`,
ADD COLUMN `source` VARCHAR(32) NOT NULL AFTER `namespace`;

ALTER TABLE `communication_engine`.`whatsapp_users`
CHANGE COLUMN `source` `source` VARCHAR(32) NULL ;

ALTER TABLE `communication_engine`.`templates`
ADD COLUMN `owner_email` VARCHAR(255) NULL AFTER `attributes`,
ADD COLUMN `vertical` VARCHAR(32) NULL AFTER `owner_email`;

ALTER TABLE `communication_engine`.`templates`
ADD COLUMN `meta_data` json DEFAULT NULL AFTER `owner_email`;
