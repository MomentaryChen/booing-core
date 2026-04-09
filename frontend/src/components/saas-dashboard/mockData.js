/** Demo-only metrics for SaaS dashboard UI showcase. */

export const kpiMetrics = [
  { id: "rev", label: "Monthly recurring revenue", value: "$48,290", delta: "+12.4%", trend: "up" },
  { id: "users", label: "Active users", value: "12,847", delta: "+3.1%", trend: "up" },
  { id: "conv", label: "Trial → paid", value: "18.2%", delta: "−0.6%", trend: "down" },
  { id: "api", label: "API requests (24h)", value: "2.4M", delta: "+8.0%", trend: "up" },
];

export const trafficSeries = [
  { day: "Mon", visits: 4200 },
  { day: "Tue", visits: 5100 },
  { day: "Wed", visits: 4800 },
  { day: "Thu", visits: 6200 },
  { day: "Fri", visits: 5900 },
  { day: "Sat", visits: 3200 },
  { day: "Sun", visits: 2800 },
];

export const recentEvents = [
  { id: "1", time: "14:32:08", event: "checkout.session.completed", user: "user_8f3a…", status: "ok" },
  { id: "2", time: "14:31:52", event: "customer.subscription.updated", user: "user_2b91…", status: "ok" },
  { id: "3", time: "14:29:01", event: "invoice.payment_failed", user: "user_c441…", status: "warn" },
  { id: "4", time: "14:27:44", event: "payment_intent.succeeded", user: "user_9aa0…", status: "ok" },
  { id: "5", time: "14:25:12", event: "account.updated", user: "user_77ce…", status: "ok" },
  { id: "6", time: "14:22:03", event: "charge.dispute.created", user: "user_01fd…", status: "err" },
];
