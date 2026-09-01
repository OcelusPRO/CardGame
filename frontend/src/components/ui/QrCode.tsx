import { useEffect, useState } from 'react'
import QRCode from 'qrcode'

interface Props {
  value: string
  size?: number
}

/**
 * The join link as a QR code, drawn in the browser so the server never has to render
 * an image. Friends point a phone at it and they are in.
 */
export function QrCode({ value, size = 180 }: Props) {
  const [dataUrl, setDataUrl] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    QRCode.toDataURL(value, {
      width: size * 2,
      margin: 1,
      color: { dark: '#150726', light: '#fdf8f3' },
    })
      .then((url) => {
        if (!cancelled) setDataUrl(url)
      })
      .catch(() => setDataUrl(null))
    return () => {
      cancelled = true
    }
  }, [value, size])

  if (!dataUrl) {
    return <div style={{ width: size, height: size }} className="sketch animate-pulse bg-ink/5" />
  }

  return (
    <img
      src={dataUrl}
      width={size}
      height={size}
      alt="QR code pour rejoindre la partie"
      className="sketch bg-paper p-2 shadow-card"
    />
  )
}
