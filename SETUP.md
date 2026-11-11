# Hopey & Charlie's Hearts — setup

## Local run

- Java 21 or newer
- Run `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)
- The default local database is a persistent H2 file under `./data/`.
- The homepage is served at `http://localhost:8080/`.

## Required environment variables before deployment

Set these in your hosting provider's environment-variable settings. Do not commit passwords or app passwords to Git.

| Variable | Value |
|---|---|
| `ADMIN_PASSWORD` | A strong, unique administrator password. Admin login is disabled until this is set. |
| `JWT_SECRET` | A unique random signing secret of at least 32 characters. |
| `MAIL_USERNAME` | `test@gmail.com` is retained as the requested placeholder. Replace it with the complete Gmail address you will send from. |
| `MAIL_PASSWORD` | The Google App Password for that Gmail account, not the normal Google account password. |
| `ADMIN_EMAIL` | Inbox that receives new-donation alerts and the email configuration test. Defaults to `MAIL_USERNAME`. |
| `BITCOIN_WALLET` | The receiving Bitcoin address. Verify it carefully before accepting donations. |

For Gmail, enable 2-Step Verification on the sending Google account, create an App Password in Google Account security settings, and place that value in `MAIL_PASSWORD`. Google may not offer App Passwords for every account or account configuration.

After deployment, log in to the admin panel and call `POST /api/admin/email/test` with the `x-admin-token` header returned by admin login. A successful response confirms that the application could send a test message to `ADMIN_EMAIL`. If it fails, inspect the application logs and verify the environment variables.

## Database

For a hosted PostgreSQL database, set:

- `SPRING_DATASOURCE_URL` to a JDBC URL such as `jdbc:postgresql://HOST:5432/DATABASE`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_JPA_DDL_AUTO` to `update` for this current schema, or use managed migrations before production.

The PostgreSQL driver is already included. If no hosted database variables are supplied, local development uses the persistent H2 file database.

## Donation and payment safety

- Bitcoin submissions are recorded as **pending** until an administrator verifies the transaction on the blockchain.
- Donors can provide a transaction hash so the administrator can locate the transfer.
- The displayed donation amount is USD; the donor must send the equivalent BTC amount.
- Card payments are intentionally marked as coming soon. This project no longer requests or emails photographs of payment cards. Add card payments only through a PCI-compliant payment processor.
- A donation record is not a payment receipt and must never be described as confirmed until funds are independently verified.

## Important before public launch

Confirm the organisation's legal status, beneficiary arrangements, use-of-funds policy, privacy notice, contact details, and any tax-deductibility statements before publishing them. Do not publish invented impact figures, testimonials, or claims that donations are tax deductible.
