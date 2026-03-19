const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.database();

/**
 * Triggered when a new message is written to messages/{conversationId}/{messageId}
 * Sends a push notification to the recipient user.
 */
exports.onNewMessage = functions.database
  .ref("/messages/{conversationId}/{messageId}")
  .onCreate(async (snapshot, context) => {
    const message = snapshot.val();
    if (!message) return null;

    const senderId = message.senderId;
    const receiverId = message.receiverId;
    if (!receiverId || receiverId === senderId) return null;

    // Get receiver's FCM token
    const receiverSnap = await db.ref(`users/${receiverId}`).once("value");
    const receiver = receiverSnap.val();
    if (!receiver || !receiver.fcmToken) return null;

    // Get sender's username
    const senderSnap = await db.ref(`users/${senderId}`).once("value");
    const sender = senderSnap.val();
    const senderName = sender?.username || "Alguém";

    // Build notification body
    let body;
    switch (message.type) {
      case "IMAGE": body = "📷 Imagem"; break;
      case "AUDIO": body = "🎤 Áudio"; break;
      case "VIDEO": body = "🎬 Vídeo"; break;
      case "LOCATION": body = "📍 Localização"; break;
      default:
        // Decrypt if needed — for simplicity show generic text if content looks encrypted
        body = message.content && message.content.length < 200
          ? message.content
          : "Nova mensagem";
    }

    const payload = {
      notification: {
        title: senderName,
        body: body,
      },
      data: {
        senderId: senderId,
        conversationId: context.params.conversationId,
        type: "direct_message",
      },
      token: receiver.fcmToken,
    };

    try {
      await admin.messaging().send(payload);
    } catch (err) {
      console.error("Error sending notification:", err);
    }
    return null;
  });

/**
 * Triggered when a new group message is written to groupMessages/{groupId}/{messageId}
 * Sends a push notification to all group members except the sender.
 */
exports.onNewGroupMessage = functions.database
  .ref("/groupMessages/{groupId}/{messageId}")
  .onCreate(async (snapshot, context) => {
    const message = snapshot.val();
    if (!message) return null;

    const senderId = message.senderId;
    const groupId = context.params.groupId;

    // Get group info to find members
    const groupSnap = await db.ref(`groups/${groupId}`).once("value");
    const group = groupSnap.val();
    if (!group || !group.members) return null;

    const members = group.members;
    const recipients = members.filter((uid) => uid !== senderId);
    if (recipients.length === 0) return null;

    // Get sender's username
    const senderSnap = await db.ref(`users/${senderId}`).once("value");
    const sender = senderSnap.val();
    const senderName = sender?.username || "Alguém";
    const groupName = group.name || "Grupo";

    let body;
    switch (message.type) {
      case "IMAGE": body = `${senderName}: 📷 Imagem`; break;
      case "AUDIO": body = `${senderName}: 🎤 Áudio`; break;
      case "VIDEO": body = `${senderName}: 🎬 Vídeo`; break;
      case "LOCATION": body = `${senderName}: 📍 Localização`; break;
      default:
        body = `${senderName}: ${message.content || "Nova mensagem"}`;
    }

    // Get FCM tokens for all recipients
    const tokenPromises = recipients.map((uid) =>
      db.ref(`users/${uid}/fcmToken`).once("value")
    );
    const tokenSnaps = await Promise.all(tokenPromises);
    const tokens = tokenSnaps
      .map((s) => s.val())
      .filter((t) => t != null);

    if (tokens.length === 0) return null;

    // Send multicast
    const multicastMessage = {
      notification: {
        title: groupName,
        body: body,
      },
      data: {
        senderId: senderId,
        groupId: groupId,
        type: "group_message",
      },
      tokens: tokens,
    };

    try {
      await admin.messaging().sendEachForMulticast(multicastMessage);
    } catch (err) {
      console.error("Error sending group notification:", err);
    }
    return null;
  });

