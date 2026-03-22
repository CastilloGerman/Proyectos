/** Metadatos enviados al API en login/registro para enriquecer `usuario_sesion`. */
export interface DeviceClientInfoPayload {
  deviceLabel: string;
  platform?: string;
  vendor?: string;
}

/**
 * Resume el dispositivo/navegador en el cliente; el servidor sigue parseando User-Agent e IP.
 */
export function buildDeviceClientInfo(): DeviceClientInfoPayload {
  if (typeof navigator === 'undefined') {
    return { deviceLabel: 'Desconocido' };
  }
  const ua = navigator.userAgent || '';
  const tablet = /iPad|Tablet|Android(?!.*Mobile)/i.test(ua);
  const mobile = /Mobi|Android|iPhone|iPod/i.test(ua);
  let formFactor = 'PC / escritorio';
  if (tablet) {
    formFactor = 'Tablet';
  } else if (mobile) {
    formFactor = 'Móvil';
  }

  const nav = navigator as Navigator & {
    userAgentData?: { platform?: string; brands?: { brand: string; version?: string }[] };
  };
  const platform = nav.userAgentData?.platform;
  const brand = nav.userAgentData?.brands?.find((b) => !/Not.?A.?Brand/i.test(b.brand))?.brand;

  let browserGuess = '';
  if (/Edg\//i.test(ua)) {
    browserGuess = 'Edge';
  } else if (/Chrome|CriOS/i.test(ua) && !/Edg/i.test(ua)) {
    browserGuess = 'Chrome';
  } else if (/Firefox|FxiOS/i.test(ua)) {
    browserGuess = 'Firefox';
  } else if (/Safari/i.test(ua) && !/Chrome|CriOS/i.test(ua)) {
    browserGuess = 'Safari';
  }

  const parts = [formFactor, browserGuess || brand, platform].filter((p) => !!p && String(p).trim().length > 0);
  const deviceLabel = parts.length > 0 ? parts.join(' · ') : 'Navegador web';

  return {
    deviceLabel,
    platform: platform ?? undefined,
    vendor: brand,
  };
}
