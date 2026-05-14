'use client'
import { useState, useEffect, useRef } from 'react'
import { cn } from '@/shared/lib/utils'
import { formatBRL } from '@/shared/lib/formatters'

interface MoneyInputProps {
  value: number
  onChange: (value: number) => void
  disabled?: boolean
  className?: string
  id?: string
  'aria-label'?: string
  'aria-describedby'?: string
}

function MoneyInput({
  value,
  onChange,
  disabled,
  className,
  id,
  'aria-label': ariaLabel,
  'aria-describedby': ariaDescribedby,
}: MoneyInputProps) {
  const [focused, setFocused] = useState(false)
  const [rawDigits, setRawDigits] = useState<string>(() => {
    const cents = Math.round(value * 100)
    return cents > 0 ? String(cents) : ''
  })

  // Sync rawDigits when value changes externally
  const prevValue = useRef(value)
  useEffect(() => {
    if (!focused && value !== prevValue.current) {
      const cents = Math.round(value * 100)
      setRawDigits(cents > 0 ? String(cents) : '')
      prevValue.current = value
    }
  }, [value, focused])

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const filtered = e.target.value.replace(/\D/g, '')
    setRawDigits(filtered)
    const numericValue = filtered.length > 0 ? Number(filtered) / 100 : 0
    prevValue.current = numericValue
    onChange(numericValue)
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    const allowed = [
      'Backspace', 'Delete', 'Tab', 'Enter',
      'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown',
    ]
    if (allowed.includes(e.key)) return
    if (e.key >= '0' && e.key <= '9') return
    e.preventDefault()
  }

  function handleFocus() {
    setFocused(true)
  }

  function handleBlur() {
    setFocused(false)
    prevValue.current = value
  }

  const displayValue = focused
    ? rawDigits
    : value > 0
    ? formatBRL(value)
    : 'R$ 0,00'

  return (
    <div className={cn('relative flex items-center', className)}>
      {!focused && (
        <span className="pointer-events-none absolute left-2.5 text-sm text-muted-foreground select-none">
        </span>
      )}
      <input
        id={id}
        type="text"
        inputMode="numeric"
        value={displayValue}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        onFocus={handleFocus}
        onBlur={handleBlur}
        disabled={disabled}
        aria-label={ariaLabel}
        aria-describedby={ariaDescribedby}
        className={cn(
          'h-8 w-full min-w-0 rounded-lg border border-input bg-transparent px-2.5 py-1 text-base transition-colors outline-none',
          'placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50',
          'disabled:pointer-events-none disabled:cursor-not-allowed disabled:bg-input/50 disabled:opacity-50',
          'aria-invalid:border-destructive aria-invalid:ring-[3px] aria-invalid:ring-destructive/20',
          'md:text-sm dark:bg-input/30 dark:disabled:bg-input/80 dark:aria-invalid:border-destructive/50 dark:aria-invalid:ring-destructive/40',
        )}
      />
    </div>
  )
}

export { MoneyInput }
export default MoneyInput
