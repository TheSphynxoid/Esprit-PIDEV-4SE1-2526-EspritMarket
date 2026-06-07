import { Component, Input, inject, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';

export interface DeliveryFormData {
  deliverytype: string;
  status: string;
  cancellationReason: string;
  deliverydate: string;
  address: string;
  orderId?: number | null;
}

export interface DeliveryOrderOption {
  id: number;
  label: string;
  shippingAddress?: string;
}

@Component({
  selector: 'app-delivery-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './delivery-form.component.html',
  styleUrls: ['./delivery-form.component.css']
})
export class DeliveryFormComponent implements OnChanges {

  @Input() orders: DeliveryOrderOption[] = [];
  @Input() initialOrderId: number | null = null;

  private fb = inject(FormBuilder);
  form: FormGroup;
  readonly minDeliveryDate: string;
  readonly maxDeliveryDate: string;
  delegations: string[] = [];
  isOrderSelected = false;  // Tracer si une commande est sélectionnée

  governorates = [
    { name: 'Ariana', delegations: ['Ariana','Ettadhamen','Kalaat El Andalous','Mnihla','Raoued','Sidi Thabet','Soukra'] },
    { name: 'Béja', delegations: ['Amdoun','Béja','Goubellat','Majaz al Bab','Nefza','Teboursouk','Testour','Thibar'] },
    { name: 'Ben Arous', delegations: ['Ben Arous','Bou Mhel el-Bassatine','El Mourouj','Ezzahra','Fouchana','Hammam Chott','Hammam Lif','Mohamedia','Medina Jedida','Megrine','Mornag','Rades'] },
    { name: 'Bizerte', delegations: ['Bizerte','Djoumime','El Alia','Ghar El Melh','Ghezala','Mateur','Menzel Bourguiba','Menzel Jemil','Ras Jebel','Sejenane','Tinja','Utica','Zarzouna'] },
    { name: 'Gabès', delegations: ['Gabès','Ghannouch','Hamma','Mareth','Matmata','Menzel Habib','Metouia'] },
    { name: 'Gafsa', delegations: ['Belkhir','Gafsa','Guetar','Ksar','Mdhila','Metlaoui','Oum Larais','Redeyef','Sened','Sidi Aich'] },
    { name: 'Jendouba', delegations: ['Aïn Draham','Balta','Bousalem','Fernana','Ghardimaou','Jendouba','Jendouba Nord','Oued Mliz','Tabarka'] },
    { name: 'Kairouan', delegations: ['Alaa','Bouhajla','Chebika','Chrarda','Haffouz','Hajeb El Ayoun','Kairouan North','Kairouan South','Nasrallah','Oueslatia','Sbikha'] },
    { name: 'Kasserine', delegations: ['Ayoun','Ezzouhour','Feriana','Foussana','Hassi El Ferid','Hidra','Jedeliane','Kasserine North','Kasserine South','Majel Belabbes','Sbeitla','Sbiba','Thala'] },
    { name: 'Kebili', delegations: ['Douz North','Douz South','Faouar','Kebili North','Kebili South','Souk El Ahed'] },
    { name: 'Kef', delegations: ['Dahmani','Es Sers','Jerissa','Kalaa Khasbat','Kalaat Senane','Kef East','Kef West','Ksour','Nebeur','Sakiet Sidi Youssef','Tajerouine'] },
    { name: 'Mahdia', delegations: ['Boumerdes','Chebba','Chorbane','El Djem','Hbira','Ksour Essef','Mahdia','Melloulech','Ouled Chamekh','Sidi Alouane','Souassi'] },
    { name: 'Manouba', delegations: ['Borj El Amri','Douar Hicher','El Battan','Jedaida','Manouba','Mornaguia','Oued Ellil','Tebourba'] },
    { name: 'Médenine', delegations: ['Ben Guerdane','Beni Khedache','Djerba Ajim','Djerba Midoun','Djerba Houmt Souk','Medenine North','Medenine South','Sidi Makhlouf','Zarzis'] },
    { name: 'Monastir', delegations: ['Bekalta','Bembla','Beni Hassen','Jammel','Ksar Hellal','Ksibet El Mediouni','Moknine','Monastir','Ouerdanine','Sahline','Sayada-Lamta-Bou Hjar','Teboulba','Zeramdine'] },
    { name: 'Nabeul', delegations: ['Beni Khalled','Beni Khiar','Bou Argoub','Dar Chaabane El Fehri','El Mida','Grombalia','Hammam Ghezaz','Hammamet','Haouaria','Kelibia','Korba','Menzel Bouzelfa','Menzel Temime','Nabeul','Soliman','Takelsa'] },
    { name: 'Sfax', delegations: ['Agareb','Bir Ali Ben Khelifa','El Amra','El Ghraiba','Hencha','Jebeniana','Kerkennah','Mahres','Menzel Chaker','Sakiet Eddaier','Sakiet Ezzit','Sfax Medina','Sfax West','Sfax South','Skhira','Thyna'] },
    { name: 'Sidi Bouzid', delegations: ['Bir El Hfay','Jelma','Mazzouna','Meknassi','Menzel Bouzaiene','Ouled Haffouz','Regueb','Sabalat Ouled Asker','Sidi Ali Ben Aoun','Sidi Bouzid East','Sidi Bouzid West','Souk Jedid'] },
    { name: 'Siliana', delegations: ['Bargou','Bouarada','El Aroussa','El Krib','Gaafour','Kesra','Makthar','Rouhia','Sidi Bourouis','Siliana North','Siliana South'] },
    { name: 'Sousse', delegations: ['Akouda','Bouficha','Enfidha','Hammam Sousse','Hergla','Kalaa Kebira','Kalaa Sghira','Kondar','M’Saken','Sidi Bou Ali','Sidi El Heni','Sousse Jaouhara','Sousse Medina','Sousse Riadh','Sousse Sidi Abdelhamid','Zaouiet Ksibet Thrayet'] },
    { name: 'Tataouine', delegations: ['Bir Lahmar','Dhiba','Ghomrassen','Remada','Samar','Tataouine'] },
    { name: 'Tozeur', delegations: ['Degueche','Hazoua','Nefta','Tameghza','Tozeur'] },
    { name: 'Tunis', delegations: ['Bab Bhar','Bab Souika','Carthage','El Khadra','Djebel Djelloud','El Haraïria','El Kabaria','El Menzah','El Omrane','El Omrane Supérieur','El Ouardia','Ettahrir','Ezzouhour','La Goulette','La Marsa','Le Bardo','Le Kram','Medina','Sidi El Béchir','Sidi Hassine','Sijoumi'] },
    { name: 'Zaghouan', delegations: ['Bir Mchergua','Fahs','Nadhour','Saouaf','Zaghouan','Zriba'] }
  ];

  constructor() {
    const today = new Date();
    const endOfYear = new Date(today.getFullYear(), 11, 31);
    this.minDeliveryDate = this.formatAsDateInput(today);
    this.maxDeliveryDate = this.formatAsDateInput(endOfYear);

    this.form = this.fb.group({
      deliverytype: ['', Validators.required],
      status: ['Pending', [Validators.required, Validators.pattern(/^(Pending|In progress|Delivered|Cancelled)$/)]],
      cancellationReason: [''],
      deliverydate: ['', [Validators.required, this.deliveryDateRangeValidator()]],
      address: ['', Validators.required],
      orderId: [this.initialOrderId]
    });

    this.form.get('status')?.valueChanges.subscribe((status) => {
      const cancellationReasonControl = this.form.get('cancellationReason');
      if (!cancellationReasonControl) {
        return;
      }

      if (status === 'Cancelled') {
        cancellationReasonControl.setValidators([Validators.required, Validators.minLength(3)]);
      } else {
        cancellationReasonControl.clearValidators();
        cancellationReasonControl.setValue('');
      }

      cancellationReasonControl.updateValueAndValidity({ emitEvent: false });
    });

    // Écouter les changements du champ orderId et remplir l'adresse automatiquement
    this.form.get('orderId')?.valueChanges.subscribe((orderId) => {
      this.onOrderSelected(orderId);
    });
  }

  get f() {
    return this.form.controls;
  }

  onGovernorateChange(): void {
    const selectedGov = this.form.get('governorate')?.value;
    const gov = this.governorates.find(g => g.name === selectedGov);
    this.delegations = gov ? gov.delegations : [];
    this.form.get('delegation')?.setValue(''); // réinitialise la délégation
  }

  onOrderSelected(orderId: number | null): void {
    if (!orderId) {
      this.form.get('address')?.setValue('');
      return;
    }

    const selectedOrder = this.orders.find(o => o.id === orderId);
    if (!selectedOrder || !selectedOrder.shippingAddress) {
      return;
    }

    // Remplir directement le champ address avec l'adresse de la commande
    this.form.patchValue({
      address: selectedOrder.shippingAddress
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['initialOrderId'] && this.form) {
      const newOrderId = changes['initialOrderId'].currentValue;
      this.form.get('orderId')?.setValue(newOrderId);
    }
  }

  getFormData(): DeliveryFormData {
    const f = this.form.value;
    return {
      deliverytype: f.deliverytype,
      status: f.status,
      cancellationReason: f.cancellationReason,
      deliverydate: f.deliverydate,
      address: f.address,
      orderId: f.orderId
    };
  }

  setFormData(data: Partial<DeliveryFormData>): void {
    const updateData: any = {};
    if (data.deliverytype !== undefined) updateData.deliverytype = data.deliverytype;
    if (data.status !== undefined) updateData.status = data.status;
    if (data.cancellationReason !== undefined) updateData.cancellationReason = data.cancellationReason;
    if (data.deliverydate !== undefined) updateData.deliverydate = data.deliverydate;
    if (data.address !== undefined) updateData.address = data.address;
    if (data.orderId !== undefined) updateData.orderId = data.orderId;

    this.form.patchValue(updateData);
  }

  resetForm(): void {
    this.form.reset({
      deliverytype: '',
      status: 'Pending',
      cancellationReason: '',
      deliverydate: '',
      address: '',
      orderId: this.initialOrderId ?? null
    });
  }

  isFormValid(): boolean {
    return this.form.valid;
  }

  private deliveryDateRangeValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const rawValue = String(control.value ?? '').trim();
      if (!rawValue) {
        return null;
      }

      // Use yyyy-mm-dd lexical comparison (same format as input[type="date"]).
      if (rawValue < this.minDeliveryDate) {
        return { minDate: true };
      }

      if (rawValue > this.maxDeliveryDate) {
        return { maxDate: true };
      }

      return null;
    };
  }

  private formatAsDateInput(value: Date): string {
    const year = value.getFullYear();
    const month = String(value.getMonth() + 1).padStart(2, '0');
    const day = String(value.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}