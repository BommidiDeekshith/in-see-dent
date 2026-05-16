export default function SeverityBadge({ severity }: { severity: string }) {
  const cls = `badge-${severity.toLowerCase()}`;
  return <span className={cls}>{severity}</span>;
}
