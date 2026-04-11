# Console Audit & Live Monitoring

## Overview
Standard server logs (`System.out.println`) are often too noisy or too quiet for real-time moderation. Ub3r uses a **Categorized Audit System** powered by Log4j2.

## 1. Audit Categories
Each major social and economic action has a dedicated logger:
- `net.dodian.consoleaudit.trade`: Every item added/removed and every finalized trade.
- `net.dodian.consoleaudit.duel`: Every duel stake and outcome.
- `net.dodian.consoleaudit.shop`: Every purchase and sale (including prices).
- `net.dodian.consoleaudit.chat`: Every public and private message.
- `net.dodian.consoleaudit.item`: Every item drop, pickup, and "Use Item On" action.

## 2. Performance Implementation
The audit system is **Asynchronous**. 
- When a trade is finalized, the `TradeLog` calls `logger.info()`.
- The message is placed in a memory buffer.
- A background "Log4j-Worker" thread writes the message to the disk (`logs/console-audit.log`).
- **Impact**: Zero performance cost to the 600ms game tick, even during high-volume trading.

## 3. Level Management
Administrators can toggle these logs on or off without restarting the server by changing environment variables or the `log4j2.xml` file.
- `DODIAN_CONSOLE_AUDIT_ITEM_LEVEL=info`: Shows every drop.
- `DODIAN_CONSOLE_AUDIT_ITEM_LEVEL=off`: Silences drop logs to save disk space.

## 4. Evidence
All audit logic is centralized in the `net.dodian.uber.game.persistence.audit` package in Kotlin.
- `ChatLog.kt`
- `TradeLog.kt`
- `DuelLog.kt`
- `ShopLog.kt`
- `ItemLog.kt`
