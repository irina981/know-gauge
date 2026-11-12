---
title: Data Classification and Handling Standard
pdf: Corvane - Data Classification and Handling Standard.pdf
docid: SEC-007
version: 2.0
effective: 1 January 2026
owner: Chief Information Security Officer (CISO), with the Data Protection Officer
appliesTo: All information created, received or stored by Corvane, in any format
---

# Data Classification and Handling Standard

## 1. Purpose

Not all information needs the same protection. This standard defines four classification levels and the minimum
handling rules for each one, so that everyone protects information in proportion to the harm its loss would cause.

## 2. The four classification levels

| Level | Definition | Examples |
|---|---|---|
| **Public** | Approved for anyone to see. Disclosure causes no harm. | Published website content, press releases, public product documentation, job adverts. |
| **Internal** | For Corvane employees and contractors. Disclosure would cause minor harm. | Intranet pages, org charts, internal process guides, most internal meeting notes. |
| **Confidential** | Limited to people who need it for their work. Disclosure would cause significant harm to Corvane, customers or individuals. | Customer contracts, pricing, source code, product roadmaps, customer personal data, employee performance reviews. |
| **Restricted** | Limited to named individuals. Disclosure would cause severe harm, legal penalties or loss of customer trust. | Encryption keys and passwords, payment card data, health and other special-category personal data, merger and acquisition plans, unreleased financial results, security vulnerability details. |

**Default rule:** information without a label is treated as **Internal**. When information combines several levels,
the highest level applies to the whole item. Personal data about customers or employees is always at least
**Confidential**.

## 3. Roles

- **Data owner**: the business leader accountable for a set of information (for example the Head of Sales owns CRM
  data). The owner decides the classification, approves access and reviews it at least once a year.
- **Data custodian**: the team that operates the systems that store the information (usually IT or Engineering) and
  applies the technical controls.
- **Data user**: anyone who uses the information. Users must follow the handling rules and report misuse.

## 4. Labelling

- Documents and presentations: use the sensitivity label in Microsoft Office. The label adds the classification to the
  header or footer automatically.
- Email: choose the label in Outlook. Confidential and Restricted emails show **[CONFIDENTIAL]** or **[RESTRICTED]**
  at the start of the subject line.
- Systems and databases: the data owner records the classification in the Corvane system inventory.
- Public and Internal items do not need a visible label, but may have one.

## 5. Handling rules

| Activity | Internal | Confidential | Restricted |
|---|---|---|---|
| Who may access | All employees and contractors | People with a business need, approved by the data owner | Named individuals approved in writing by the data owner |
| Sharing outside Corvane | Allowed for business purposes | Only with an NDA or contract in place, via Corvane SecureShare | Only with data owner approval, via Corvane SecureShare with a link expiry of at most 14 days |
| Email to external addresses | Allowed | Allowed only to recipients covered by an NDA; attachments must be encrypted | Not allowed; use SecureShare |
| Storage | Approved Corvane locations | Approved Corvane locations with access restricted to the team | Approved systems listed for Restricted data only; encrypted at rest |
| Printing | Allowed | Allowed; collect prints immediately with secure print release | Not allowed without data owner approval |
| Generative AI | Corvane Assist only | Corvane Assist only | Not allowed in any AI tool |
| Disposal | Normal recycling for paper | Shred bins or secure deletion | Shred bins or secure deletion, logged by IT for devices |

Public information can be shared freely, but only the Communications team can approve that something new becomes
Public.

## 6. Retention and disposal

Keep information only as long as it is needed for its purpose or required by law, as set in the Corvane Retention
Schedule. For example, invoices are kept for **7 years**, recruitment data about unsuccessful candidates for
**4 weeks** after the vacancy closes (or **1 year** with the candidate's consent), and CCTV footage for **28 days**.

Paper containing Confidential or Restricted information goes in the locked **shred bins**, never in normal recycling.
Laptops, phones and disks are returned to IT, who wipe or destroy them with a certified process and keep a disposal
record.

## 7. Clean desk and clear screen

- At the end of the day, and whenever you leave your desk for more than **30 minutes**, lock away any Confidential or
  Restricted paper documents.
- Wipe whiteboards after meetings that discussed Confidential or Restricted information.
- Do not leave documents on printers or in meeting rooms.
- Use a privacy screen filter when working with Confidential or Restricted information in public places such as
  trains and cafés.

## 8. Reclassification

Classification can change over time. For example, quarterly results are Restricted until they are published, and then
become Public. Only the data owner can lower a classification. Anyone can raise it if they believe it is too low,
and should inform the data owner.

## 9. Incidents

If Confidential or Restricted information is sent to the wrong person, lost or exposed, report it to the Security Desk
(ext. 4444) immediately, and at the latest within **1 hour**. It may be a personal data breach that Corvane has to
report to the authorities within 72 hours (see the Personal Data Breach Response Procedure).
