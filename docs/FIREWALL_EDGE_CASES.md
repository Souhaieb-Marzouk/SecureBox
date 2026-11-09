# Firewall edge cases

SecureBox 0.9 uses one all-app `VpnService`. Allowed IPv4 TCP/UDP flows are forwarded through
protected sockets; blocked apps’ TCP connections are reset and their UDP packets are dropped; DNS is
intercepted and forwarded over DoH. The tunnel is IPv4-only in v0.9.5; IPv6 is not filtered. This
document records how the combined engine behaves in the situations that usually surprise users.

## Private DNS (DNS over TLS)

**Behaviour:** blocked apps lose all internet access, including DNS. Allowed apps use the tunnel’s
DoH resolver unless Private DNS is configured, in which case Android may prefer Private DNS and
bypass SecureBox’s DNS filter.

**What the user sees:** a blocked app behaves as if it is offline. If an allowed app still shows ads,
turn Private DNS to Automatic/Off or pick a blocking Private DNS provider.

## Another VPN is active

Android permits one VPN at a time. If another VPN is connected, `VpnService.Builder.establish()`
fails or the system routes traffic through the other VPN.

**Behaviour:** the Network screen shows "Another VPN is active…" and the firewall does not start.
SecureBox does not try to fight the other VPN or silently disable it.

## Always-on VPN / "Block connections without VPN"

If another app is configured as always-on, it owns the VPN slot.

**Behaviour:** same as above — the conflict banner appears. If SecureBox itself is set as
always-on, Android may start the service before the user unlocks the device; the service waits for
the state flow and establishes when either the Local firewall or DNS protection is enabled. When
both are off, it does not create a tunnel.

## Work profile and secondary users

Android runs work profiles and secondary users in separate package spaces with separate UIDs.

**Behaviour:** SecureBox sees and blocks apps in its own profile only. An app installed in a work
profile is invisible to the main profile's firewall and must be managed by a SecureBox install in
that profile. This is a platform boundary, not a bug.

## Per-app VPN apps

Some apps (enterprise clients) create their own per-app VPN. They still occupy the single VPN slot.

**Behaviour:** detected as "another VPN"; the firewall reports the conflict instead of failing
silently.

## Reboot

The VPN does not survive a reboot. The firewall's enabled flag and blocklist persist, but the
tunnel is only recreated when the service is started again — currently when the user opens the app
or toggles the firewall. Always-on VPN configured in system settings can restart it automatically.

## Uninstalling a blocked app

The service rebuilds its allowed-application list when the blocklist changes. If an app disappears
while the tunnel is running, the next rebuild simply skips it. If *every* blocked app is gone, the
service stops instead of creating a tunnel that would capture all traffic.

## What the firewall does not do

- It does not filter by domain, port or protocol. Domain blocking needs a forwarding-DNS design and
  is not implemented; the threat feed's `domains` list is stored but not enforced.
- It does not proxy or inspect payloads; it drops packets.
- It does not block apps that use a different user profile or a separate VPN.
