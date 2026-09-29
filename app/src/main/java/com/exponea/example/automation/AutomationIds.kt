package com.exponea.example.automation

object AutomationIds {
    // Loading screen
    const val LOADING_LOGO = "img_loading_logo"

    // Authentication screen
    const val AUTH_TITLE = "lbl_auth_title"
    const val AUTH_MODE_PROJECT = "btn_auth_mode_project"
    const val AUTH_MODE_STREAM = "btn_auth_mode_stream"
    const val AUTH_PROJECT_TOKEN = "fld_auth_project_token"
    const val AUTH_AUTHORIZATION_TOKEN = "fld_auth_authorization_token"
    const val AUTH_ADVANCED_AUTH = "fld_auth_advanced_auth"
    const val AUTH_STREAM_ID = "fld_auth_stream_id"
    const val AUTH_JWT_KEY_ID = "fld_auth_jwt_key_id"
    const val AUTH_JWT_SECRET = "fld_auth_jwt_secret"
    const val AUTH_REGISTERED_ID = "fld_auth_registered_id"
    const val AUTH_API_URL = "fld_auth_api_url"
    const val AUTH_APPLICATION_ID = "fld_auth_application_id"
    const val AUTH_START = "btn_auth_start"
    const val AUTH_CLEAR_LOCAL_DATA = "btn_auth_clear_local_data"
    const val AUTH_ERROR_OK = "btn_auth_error_ok"

    // App Shell — AppBar + Bottom Nav
    const val STATUS_SDK_INDICATOR = "status_sdk_indicator"
    const val OPEN_CONFIG_DRAWER = "btn_open_config_drawer"
    const val CLOSE_CONFIG_DRAWER = "btn_close_config_drawer"
    const val CONFIG_DRAWER = "drawer_sdk_config"
    const val OPEN_LOGS = "btn_open_logs"
    const val NAV_FETCHING = "btn_nav_fetching"
    const val NAV_TRACKING = "btn_nav_tracking"
    const val NAV_FLUSHING = "btn_nav_flushing"
    const val NAV_ANONYMIZE = "btn_nav_anonymize"
    const val NAV_CONTENT_BLOCKS = "btn_nav_content_blocks"

    // Fetching screen
    const val FETCH_CONSENTS = "btn_fetch_consents"
    const val FETCH_RECOMMENDATIONS = "btn_fetch_recommendations"
    const val FETCH_SEGMENTS = "btn_fetch_segments"
    const val APP_INBOX_PROVIDER = "btn_app_inbox_provider"
    const val OPEN_INBOX = "btn_open_inbox"
    const val CLEAR_RESULT = "btn_clear_result"
    const val FETCH_RESULT = "lbl_fetch_result"
    const val FETCH_LOADING = "status_fetch_loading"

    // Fetch Recommendation dialog
    const val RECOMMENDATION_ID = "fld_recommendation_id"
    const val RECOMMENDATION_DIALOG_CANCEL = "btn_recommendation_dialog_cancel"
    const val RECOMMENDATION_DIALOG_FETCH = "btn_recommendation_dialog_fetch"

    // Tracking screen
    const val COOKIE_ID = "lbl_cookie_id"
    const val COPY_COOKIE_ID = "btn_copy_cookie_id"
    const val IDENTIFY_CUSTOMER = "btn_identify_customer"
    const val SET_AUTH_TOKEN = "btn_set_auth_token"
    const val TRACK_CUSTOM_EVENT = "btn_track_custom_event"
    const val TRACK_PAYMENT = "btn_track_payment"
    const val TRACK_PAGE_VIEW = "btn_track_page_view"
    const val REQUEST_PUSH = "btn_request_push"
    const val TRACK_FCM_TOKEN = "btn_track_fcm_token"
    const val TRACK_PUSH_DELIVERED = "btn_track_push_delivered"
    const val TRACK_PUSH_CLICKED = "btn_track_push_clicked"

    // Identify Customer dialog
    const val REGISTERED_ID = "fld_registered_id"
    const val REGISTERED_ID_LABEL = "lbl_registered_id"
    const val IDENTIFY_ATTR_NAME = "fld_identify_attr_name"
    const val IDENTIFY_ATTR_VALUE = "fld_identify_attr_value"
    const val IDENTIFY_ADD_ATTR = "btn_identify_add_attr"
    const val IDENTIFY_ATTRS_JSON = "lbl_identify_attrs_json"
    const val IDENTIFY = "btn_identify"
    const val IDENTIFY_AUTH = "btn_identify_auth"

    // Track Custom Event dialog
    const val INAPP_PRESET = "sel_inapp_preset"
    const val EVENT_NAME = "fld_event_name"
    const val EVENT_ATTR_NAME = "fld_event_attr_name"
    const val EVENT_ATTR_VALUE = "fld_event_attr_value"
    const val EVENT_ADD_ATTR = "btn_event_add_attr"
    const val EVENT_PROPS_JSON = "lbl_event_props_json"
    const val TRACK_EVENT = "btn_track_event"

    // Flushing screen
    const val TAB_FLUSH_MODE = "tab_flush_mode"
    const val TAB_FLUSH_PERIOD = "tab_flush_period"
    const val TAB_LOG_LEVEL = "tab_log_level"
    const val GET_FLUSH_MODE = "btn_get_flush_mode"
    const val CURRENT_FLUSH_MODE = "lbl_current_flush_mode"
    const val FLUSH_MODE_IMMEDIATE = "btn_flush_mode_immediate"
    const val FLUSH_MODE_PERIOD = "btn_flush_mode_period"
    const val FLUSH_MODE_APP_CLOSE = "btn_flush_mode_app_close"
    const val FLUSH_MODE_MANUAL = "btn_flush_mode_manual"
    const val GET_FLUSH_PERIOD = "btn_get_flush_period"
    const val CURRENT_FLUSH_PERIOD = "lbl_current_flush_period"
    const val FLUSH_PERIOD_30 = "btn_flush_period_30"
    const val FLUSH_PERIOD_60 = "btn_flush_period_60"
    const val FLUSH_DATA = "btn_flush_data"
    const val GET_LOG_LEVEL = "btn_get_log_level"
    const val CURRENT_LOG_LEVEL = "lbl_current_log_level"
    const val LOG_OFF = "btn_log_off"
    const val LOG_ERROR = "btn_log_error"
    const val LOG_WARN = "btn_log_warn"
    const val LOG_INFO = "btn_log_info"
    const val LOG_DEBUG = "btn_log_debug"
    const val LOG_VERBOSE = "btn_log_verbose"

    // Flush Result dialog
    const val FLUSH_RESULT_TITLE = "lbl_flush_result_title"
    const val FLUSH_RESULT_OK = "btn_flush_result_ok"

    // Anonymize screen
    const val ANONYMIZE = "btn_anonymize"
    const val STOP_INTEGRATION = "btn_stop_integration"

    // Customer anonymized dialog
    const val ANONYMIZED_TITLE = "lbl_anonymized_title"
    const val ANONYMIZED_OK = "btn_anonymized_ok"

    // Stop Integration confirmation dialog
    const val STOP_CONTINUE = "btn_stop_continue"
    const val STOP_BACK_TO_AUTH = "btn_stop_back_to_auth"
    const val STOP_DIALOG_CLOSE = "btn_stop_dialog_close"

    // In-app Content Blocks screen
    const val TOGGLE_CAROUSELS = "btn_toggle_carousels"
    const val CB_PLACEHOLDER_EXAMPLE_TOP = "cb_placeholder_example_top"
    const val CB_PLACEHOLDER_EXAMPLE_PLATFORM = "cb_placeholder_example_platform"
    const val CB_PLACEHOLDER_EXAMPLE_LIST = "cb_placeholder_example_list"
    const val CB_CAROUSEL_DEFAULT = "cb_carousel_default"
    const val CB_CAROUSEL_FILTERED = "cb_carousel_filtered"
    const val CB_CAROUSEL_PLATFORM = "cb_carousel_platform"
    const val CAROUSEL_STATUS = "lbl_carousel_status"

    // App Inbox — List page
    const val APP_INBOX_TITLE = "lbl_app_inbox_title"
    const val APP_INBOX_BACK = "btn_app_inbox_back"
    const val LIST_APP_INBOX = "list_app_inbox"
    const val ROW_APP_INBOX_ITEM = "row_app_inbox_item"

    // App Inbox — Detail page
    const val APP_INBOX_DETAIL_TITLE = "lbl_app_inbox_detail_title"
    const val APP_INBOX_DETAIL_BACK = "btn_app_inbox_detail_back"
    const val VIEW_APP_INBOX_DETAIL = "view_app_inbox_detail"

    // Logs sheet
    const val LOGS_COPY = "btn_logs_copy"
    const val LOGS_CLEAR = "btn_logs_clear"
    const val LOGS_CLOSE = "btn_logs_close"
    const val LOGS_FILTER_ALL = "btn_logs_filter_all"
    const val LOGS_FILTER_VERBOSE = "btn_logs_filter_verbose"
    const val LOGS_FILTER_DEBUG = "btn_logs_filter_debug"
    const val LOGS_FILTER_INFO = "btn_logs_filter_info"
    const val LOGS_FILTER_WARN = "btn_logs_filter_warn"
    const val LOGS_FILTER_ERROR = "btn_logs_filter_error"
    const val LIST_LOGS = "list_logs"
    const val LOGS_EMPTY = "lbl_logs_empty"
}
