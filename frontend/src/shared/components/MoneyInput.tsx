'use client'
import { NumericFormat } from 'react-number-format'
import { Input } from '@/shared/components/ui/input'
import { cn } from '@/shared/lib/utils'

const MAX_VALUE = 999_999_999.99

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
  return (
    <NumericFormat
      customInput={Input}
      thousandSeparator="."
      decimalSeparator=","
      prefix="R$ "
      decimalScale={2}
      fixedDecimalScale
      allowNegative={false}
      isAllowed={(values) => {
        const { floatValue } = values
        return floatValue === undefined || floatValue <= MAX_VALUE
      }}
      value={value === 0 ? '' : value}
      onValueChange={(values) => onChange(values.floatValue ?? 0)}
      disabled={disabled}
      id={id}
      aria-label={ariaLabel}
      aria-describedby={ariaDescribedby}
      className={cn(className)}
      placeholder="R$ 0,00"
    />
  )
}

export { MoneyInput }
export default MoneyInput
