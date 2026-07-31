CREATE KEYSPACE IF NOT EXISTS chat
            WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE IF NOT EXISTS chat.conversations
(
    conversation_id uuid,
    type            text,
    title           text,
    created_by      uuid,
    created_at      timestamp,
    PRIMARY KEY (conversation_id)
);

CREATE TABLE IF NOT EXISTS chat.messages_by_conversation
(
    conversation_id uuid,
    bucket          int,
    message_id      timeuuid,
    sender_id       uuid,
    content         text,
    type            text,
    deleted         boolean,
    created_at      timestamp,
    PRIMARY KEY ((conversation_id, bucket), message_id)
) WITH CLUSTERING ORDER BY (message_id DESC);

CREATE TABLE IF NOT EXISTS chat.conversations_by_user
(
    user_id           uuid,
    conversation_id   uuid,
    conversation_type text,
    PRIMARY KEY (user_id, conversation_id)
);


CREATE TABLE IF NOT EXISTS chat.users_by_username
(
    username text,
    user_id  uuid,
    PRIMARY KEY (username)
);

CREATE TABLE IF NOT EXISTS chat.participants_by_conversation
(
    conversation_id uuid,
    user_id         uuid,
    role            text,
    custom_title    text,
    joined_at       timestamp,
    PRIMARY KEY (conversation_id, user_id)
);



CREATE TABLE IF NOT EXISTS chat.user_profiles
(
    user_id  uuid,
    username text,
    PRIMARY KEY (user_id)
);



