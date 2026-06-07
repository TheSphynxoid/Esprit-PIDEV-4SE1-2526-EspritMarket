import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

interface FooterColumn {
  title: string;
  links: Array<{ label: string; url: string }>;
}

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './footer.component.html',
  styleUrls: []
})
export class FooterComponent {

  columns: FooterColumn[] = [
    {
      title: 'Marketplace',
      links: [
        { label: 'Shop', url: '#marketplace' },
        { label: 'Categories', url: '#categories' },
        { label: 'Vendors', url: '#sellers' },
        { label: 'Promotions', url: '/tickets#promo' }
      ]
    },
    {
      title: 'Services',
      links: [
        { label: 'Projects', url: '#projects' },
        { label: 'Recruitment', url: '#recruitment' },
        { label: 'Events', url: '#events' },
        { label: 'Delivery', url: '#delivery' }
      ]
    },
    {
      title: 'Support',
      links: [
        { label: 'About', url: '#about' },
        { label: 'Contact', url: '#contact' },
        { label: 'FAQ', url: '#faq' },
        { label: 'Terms', url: '#terms' }
      ]
    }
  ];
}
