db = db.getSiblingDB('commerce_platform');

db.createCollection('users');
db.createCollection('orders');
db.createCollection('channels');
db.createCollection('audit_logs');
db.createCollection('counters');

// Users indexes
db.users.createIndex({ "email": 1 }, { unique: true });

// Orders indexes
db.orders.createIndex({ "orderNumber": 1 }, { unique: true });
db.orders.createIndex({ "channel": 1 });
db.orders.createIndex({ "status": 1 });
db.orders.createIndex({ "placedAt": -1 });
db.orders.createIndex({ "customer.email": 1 });
db.orders.createIndex({ "channel": 1, "status": 1, "placedAt": -1 }, { name: "channel_status_date" });
db.orders.createIndex({ "customer.name": "text", "orderNumber": "text", "channelOrderRef": "text" }, { name: "order_text_search" });

// Channels indexes
db.channels.createIndex({ "code": 1 }, { unique: true });

// Audit logs indexes
db.audit_logs.createIndex({ "createdAt": -1 });
db.audit_logs.createIndex({ "entityType": 1, "entityId": 1 }, { name: "entity_lookup" });
